import fs from 'node:fs';

const targets = await fetch('http://localhost:9222/json').then((response) => response.json());
const target = targets.find((item) => item.type === 'page' && item.url.startsWith('http://localhost:18080'));
if (!target) {
    throw new Error('未找到本地演示页面');
}

const ws = new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolve, reject) => {
    ws.addEventListener('open', resolve, { once: true });
    ws.addEventListener('error', reject, { once: true });
});

let nextId = 1;
const pending = new Map();
const runtimeErrors = [];
const logErrors = [];
const networkErrors = [];

ws.addEventListener('message', (event) => {
    const message = JSON.parse(event.data);
    if (message.id && pending.has(message.id)) {
        const { resolve, reject } = pending.get(message.id);
        pending.delete(message.id);
        if (message.error) reject(new Error(message.error.message));
        else resolve(message.result);
        return;
    }
    if (message.method === 'Runtime.exceptionThrown') {
        runtimeErrors.push(message.params.exceptionDetails.text || 'Runtime exception');
    }
    if (message.method === 'Log.entryAdded' && message.params.entry.level === 'error') {
        logErrors.push(message.params.entry.text);
    }
});

function send(method, params = {}) {
    const id = nextId++;
    return new Promise((resolve, reject) => {
        pending.set(id, { resolve, reject });
        ws.send(JSON.stringify({ id, method, params }));
    });
}

async function evaluate(expression) {
    const result = await send('Runtime.evaluate', {
        expression,
        awaitPromise: true,
        returnByValue: true
    });
    if (result.exceptionDetails) {
        throw new Error(result.exceptionDetails.text || '页面脚本执行失败');
    }
    return result.result.value;
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

await send('Runtime.enable');
await send('Page.enable');
await send('Log.enable');
await send('Page.reload', { ignoreCache: true });
await sleep(1800);
runtimeErrors.length = 0;
logErrors.length = 0;
networkErrors.length = 0;

const initial = await evaluate(`({
    title: document.title,
    viewport: [window.innerWidth, window.innerHeight],
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth + 1,
    knowledgeVersion: document.getElementById('kbVersion')?.textContent,
    heroEntries: document.getElementById('heroEntryCount')?.textContent
})`);

await evaluate(`(() => {
    document.getElementById('crop').value = '水稻';
    document.getElementById('part').value = '下部老叶';
    document.getElementById('growthStage').value = '分蘖期';
    document.getElementById('soilType').value = '沙质土';
    document.getElementById('environment').value = '近期降雨较多，田间湿度偏高';
    const symptoms = document.getElementById('symptoms');
    symptoms.value = '叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。';
    symptoms.dispatchEvent(new Event('input', { bubbles: true }));
    document.getElementById('diagnosisForm').requestSubmit();
    return true;
})()`);

for (let attempt = 0; attempt < 40; attempt += 1) {
    const ready = await evaluate(`!document.getElementById('results').hidden && document.getElementById('resultTitle').textContent.includes('缺')`);
    if (ready) break;
    await sleep(250);
}

await sleep(600);
const result = await evaluate(`({
    resultVisible: !document.getElementById('results').hidden,
    resultTitle: document.getElementById('resultTitle').textContent,
    topSymbol: document.getElementById('topSymbol').textContent,
    confidence: document.getElementById('topConfidence').textContent,
    candidateCount: document.querySelectorAll('.candidate-item').length,
    riskLabel: document.getElementById('riskLabel').textContent,
    chemicalItems: document.querySelectorAll('#chemicalList li').length,
    sourceCount: document.querySelectorAll('.source-item').length,
    civicText: document.getElementById('civicNote').textContent,
    formErrorVisible: !document.getElementById('formError').hidden,
    horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth + 1
})`);

await evaluate(`document.getElementById('results').scrollIntoView({ block: 'start' })`);
await sleep(500);
const screenshot = await send('Page.captureScreenshot', { format: 'png', fromSurface: true });
fs.mkdirSync('docs', { recursive: true });
fs.writeFileSync('docs/ui-smoke-result.png', Buffer.from(screenshot.data, 'base64'));

console.log(JSON.stringify({ initial, result, runtimeErrors, logErrors, networkErrors }, null, 2));
ws.close();
if (runtimeErrors.length || logErrors.length || networkErrors.length || !result.resultVisible || result.formErrorVisible) {
    process.exitCode = 1;
}


