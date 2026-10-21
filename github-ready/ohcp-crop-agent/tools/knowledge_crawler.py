#!/usr/bin/env python3
"""公开农业科普资料采集与清洗脚本。

用途：采集公开页面正文，清洗为可审核的文本切片，输出 JSONL。
注意：脚本不会自动改写正式知识库；采集结果需人工核对后合并到
data/knowledge-base.json，避免把未经审核的内容直接交给智能体。

示例：
    python tools/knowledge_crawler.py \
      --url "https://example.gov.cn/article" \
      --output data/crawled_chunks.jsonl

依赖：pip install -r tools/requirements.txt
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable
from urllib.parse import urlparse
from urllib.robotparser import RobotFileParser

import requests
from bs4 import BeautifulSoup


USER_AGENT = "OHCP-CropAgent-KnowledgeCollector/1.0 (+educational-course-project)"
DEFAULT_TIMEOUT = 20
DEFAULT_DELAY = 1.5


@dataclass(frozen=True)
class Chunk:
    source_url: str
    source_title: str
    chunk_id: str
    text: str
    fetched_at: str
    content_hash: str


def load_urls(url_args: Iterable[str], urls_file: str | None) -> list[str]:
    urls = [url.strip() for url in url_args if url.strip()]
    if urls_file:
        path = Path(urls_file)
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#"):
                urls.append(line)
    # 去重但保持输入顺序
    return list(dict.fromkeys(urls))


def can_fetch(url: str) -> bool:
    parsed = urlparse(url)
    if parsed.scheme not in {"http", "https"} or not parsed.netloc:
        return False
    robots_url = f"{parsed.scheme}://{parsed.netloc}/robots.txt"
    parser = RobotFileParser()
    parser.set_url(robots_url)
    try:
        parser.read()
        return parser.can_fetch(USER_AGENT, url)
    except Exception:
        # robots.txt 不可读时不阻断公开科普页面采集，但保留日志提示。
        print(f"[WARN] 无法读取 robots.txt：{robots_url}")
        return True


def fetch_html(url: str) -> str:
    response = requests.get(
        url,
        headers={"User-Agent": USER_AGENT, "Accept-Language": "zh-CN,zh;q=0.9"},
        timeout=DEFAULT_TIMEOUT,
    )
    response.raise_for_status()
    if not response.encoding or response.encoding.lower() == "iso-8859-1":
        response.encoding = response.apparent_encoding or "utf-8"
    return response.text


def extract_article(html: str, url: str) -> tuple[str, str]:
    soup = BeautifulSoup(html, "lxml")
    for node in soup(["script", "style", "noscript", "nav", "footer", "header", "form", "aside"]):
        node.decompose()

    title = ""
    if soup.title and soup.title.string:
        title = clean_text(soup.title.string)
    if not title:
        heading = soup.find(["h1", "h2"])
        title = clean_text(heading.get_text(" ", strip=True)) if heading else urlparse(url).netloc

    candidates = soup.select("article, main, .content, .article, .detail, #content, #article")
    root = candidates[0] if candidates else soup.body or soup
    blocks: list[str] = []
    for node in root.find_all(["h1", "h2", "h3", "h4", "p", "li"]):
        text = clean_text(node.get_text(" ", strip=True))
        if len(text) >= 15 and text not in blocks:
            blocks.append(text)
    return title, "\n".join(blocks)


def clean_text(text: str) -> str:
    text = re.sub(r"[\u200b\ufeff\xa0]", " ", text)
    text = re.sub(r"\s+", " ", text)
    text = re.sub(r"(责任编辑|来源|作者)[:：].{0,80}$", "", text)
    return text.strip()


def split_chunks(text: str, chunk_size: int = 420, overlap: int = 70) -> list[str]:
    paragraphs = [item.strip() for item in text.splitlines() if item.strip()]
    chunks: list[str] = []
    current = ""
    for paragraph in paragraphs:
        if len(current) + len(paragraph) + 1 <= chunk_size:
            current = f"{current}\n{paragraph}".strip()
            continue
        if current:
            chunks.append(current)
        if len(paragraph) <= chunk_size:
            current = paragraph
        else:
            start = 0
            while start < len(paragraph):
                end = start + chunk_size
                chunks.append(paragraph[start:end])
                start = max(end - overlap, start + 1)
            current = ""
    if current:
        chunks.append(current)
    return [chunk for chunk in chunks if len(chunk) >= 80]


def build_chunks(url: str, title: str, text: str) -> list[Chunk]:
    fetched_at = datetime.now(timezone.utc).isoformat()
    result: list[Chunk] = []
    for index, part in enumerate(split_chunks(text), start=1):
        digest = hashlib.sha256(part.encode("utf-8")).hexdigest()[:16]
        result.append(Chunk(
            source_url=url,
            source_title=title,
            chunk_id=f"{urlparse(url).netloc}-{digest}",
            text=part,
            fetched_at=fetched_at,
            content_hash=digest,
        ))
    return result


def write_jsonl(chunks: list[Chunk], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8") as handle:
        for chunk in chunks:
            handle.write(json.dumps(chunk.__dict__, ensure_ascii=False) + "\n")


def main() -> None:
    parser = argparse.ArgumentParser(description="采集公开农业科普资料并切分为 JSONL")
    parser.add_argument("--url", action="append", default=[], help="公开页面 URL，可重复传入")
    parser.add_argument("--urls-file", help="每行一个 URL 的文本文件，# 开头为注释")
    parser.add_argument("--output", default="data/crawled_chunks.jsonl", help="输出 JSONL 路径")
    parser.add_argument("--delay", type=float, default=DEFAULT_DELAY, help="请求间隔秒数")
    args = parser.parse_args()

    urls = load_urls(args.url, args.urls_file)
    if not urls:
        parser.error("至少通过 --url 或 --urls-file 提供一个公开页面 URL")

    all_chunks: list[Chunk] = []
    seen_hashes: set[str] = set()
    for url in urls:
        if not can_fetch(url):
            print(f"[SKIP] robots.txt 不允许或 URL 无效：{url}")
            continue
        try:
            print(f"[FETCH] {url}")
            html = fetch_html(url)
            title, text = extract_article(html, url)
            for chunk in build_chunks(url, title, text):
                if chunk.content_hash in seen_hashes:
                    continue
                seen_hashes.add(chunk.content_hash)
                all_chunks.append(chunk)
            print(f"[OK] {title}，新增 {len(all_chunks)} 个去重切片")
        except Exception as exc:
            print(f"[ERROR] {url}：{exc}")
        time.sleep(max(0.0, args.delay))

    output = Path(args.output)
    write_jsonl(all_chunks, output)
    print(f"[DONE] 已写入 {len(all_chunks)} 个切片：{output.resolve()}")
    print("下一步：人工核对来源与事实，再把结构化建议合并进 data/knowledge-base.json。")


if __name__ == "__main__":
    main()
