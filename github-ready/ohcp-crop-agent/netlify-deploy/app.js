const samples = {
    'rice-n': {
        crop: '水稻',
        part: '下部老叶',
        growthStage: '分蘖期',
        soilType: '沙质土',
        environment: '近期降雨较多，田间湿度偏高',
        symptoms: '水稻叶片从下部老叶开始均匀发黄，植株矮小，分蘖减少。'
    },
    'wheat-k': {
        crop: '小麦',
        part: '叶缘',
        growthStage: '灌浆期',
        soilType: '沙质土',
        environment: '近期降雨较多，田间湿度偏高',
        symptoms: '下部老叶叶缘发黄焦枯，并向叶片内部扩展，茎秆软弱，植株有倒伏迹象。'
    },
    'maize-zn': {
        crop: '玉米',
        part: '新叶',
        growthStage: '苗期',
        soilType: '石灰性土',
        environment: '低温寡照',
        symptoms: '新叶变小，节间缩短，叶片出现白条，植株生长受阻，部分幼苗呈白化状。'
    },
    'tomato-ca': {
        crop: '番茄',
        part: '果实',
        growthStage: '开花期',
        soilType: '盐渍化土',
        environment: '近期干旱，土壤墒情不足',
        symptoms: '新叶边缘卷曲焦枯，果实脐部出现褐色坏死斑，部分果实开裂。'
    }
};

const form = document.getElementById('diagnosisForm');
const symptomsInput = document.getElementById('symptoms');
const charCount = document.getElementById('charCount');
const diagnoseButton = document.getElementById('diagnoseButton');
const formError = document.getElementById('formError');
const results = document.getElementById('results');

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function setList(id, items) {
    const list = document.getElementById(id);
    list.innerHTML = '';
    (items || []).forEach((item) => {
        const li = document.createElement('li');
        li.textContent = item;
        list.appendChild(li);
    });
}

function updateCharCount() {
    charCount.textContent = `${symptomsInput.value.length} / 600`;
}

function setLoading(loading) {
    diagnoseButton.disabled = loading;
    diagnoseButton.classList.toggle('loading', loading);
    diagnoseButton.querySelector('.button-label').textContent = loading ? '正在检索本地知识库' : '开始智能诊断';
}

function showError(message) {
    formError.textContent = message;
    formError.hidden = false;
}

function clearError() {
    formError.hidden = true;
    formError.textContent = '';
}

symptomsInput.addEventListener('input', updateCharCount);
updateCharCount();

document.querySelectorAll('[data-sample]').forEach((button) => {
    button.addEventListener('click', () => {
        const sample = samples[button.dataset.sample];
        if (!sample) return;
        document.getElementById('crop').value = sample.crop;
        document.getElementById('part').value = sample.part;
        document.getElementById('growthStage').value = sample.growthStage;
        document.getElementById('soilType').value = sample.soilType;
        document.getElementById('environment').value = sample.environment;
        symptomsInput.value = sample.symptoms;
        updateCharCount();
        clearError();
        symptomsInput.focus();
        window.scrollTo({ top: form.getBoundingClientRect().top + window.scrollY - 18, behavior: 'smooth' });
    });
});

form.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearError();

    const payload = {
        crop: document.getElementById('crop').value,
        part: document.getElementById('part').value,
        symptoms: symptomsInput.value.trim(),
        growthStage: document.getElementById('growthStage').value,
        soilType: document.getElementById('soilType').value,
        environment: document.getElementById('environment').value
    };

    if (!payload.crop) {
        showError('请选择作物类型。');
        document.getElementById('crop').focus();
        return;
    }
    if (!payload.part) {
        showError('请选择症状部位。');
        document.getElementById('part').focus();
        return;
    }
    if (!payload.symptoms) {
        showError('请先描述作物症状表现。');
        symptomsInput.focus();
        return;
    }

    setLoading(true);
    try {
        const response = await fetch('/api/diagnose', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const rawBody = await response.text();
        let data = {};
        try {
            data = rawBody ? JSON.parse(rawBody) : {};
        } catch (parseError) {
            data = {};
        }
        if (!response.ok) {
            const detail = data.message
                || (Array.isArray(data.details) ? data.details.join('；') : '')
                || data.error
                || (rawBody && rawBody.length < 200 ? rawBody : '')
                || `HTTP ${response.status}`;
            throw new Error(`请求失败（${response.status}）：${detail}`);
        }
        renderDiagnosis(data);
        await loadStats();
    } catch (error) {
        showError(error.message || '网络异常，请稍后重试。');
    } finally {
        setLoading(false);
    }
});

function renderDiagnosis(data) {
    const primary = data.candidates[0];
    const advice = data.advice;
    const risk = data.risk;

    results.hidden = false;
    document.getElementById('resultTitle').textContent = `首要判断：${advice.deficiencyName}`;
    document.getElementById('resultSummary').textContent = `${data.querySummary} · 本次检索 ${data.retrieval.elapsedMs} ms · ${data.retrieval.candidatesReturned} 个候选`;
    document.getElementById('engineBadge').textContent = data.engine;

    const ring = document.getElementById('confidenceRing');
    ring.style.setProperty('--confidence', primary.confidence);
    document.getElementById('topConfidence').textContent = `${primary.confidence}%`;
    document.getElementById('topSymbol').textContent = primary.symbol;
    document.getElementById('topNutrient').textContent = `缺${primary.nutrient}（${primary.symbol}）`;
    document.getElementById('topRationale').textContent = primary.rationale;

    document.getElementById('candidateList').innerHTML = data.candidates.map((candidate, index) => `
        <div class="candidate-item">
            <span class="candidate-symbol">${escapeHtml(candidate.symbol)}</span>
            <div class="candidate-info">
                <div class="candidate-info-top">
                    <strong>${index + 1}. 缺${escapeHtml(candidate.nutrient)}</strong>
                    <small>${escapeHtml(candidate.rationale)}</small>
                </div>
                <div class="candidate-bar"><i style="width:${candidate.confidence}%"></i></div>
            </div>
            <span class="candidate-confidence">${candidate.confidence}%</span>
        </div>
    `).join('');

    const riskCard = document.getElementById('riskCard');
    riskCard.classList.remove('low', 'medium', 'high');
    riskCard.classList.add(risk.level.toLowerCase());
    document.getElementById('riskLabel').textContent = risk.label;
    document.getElementById('riskScore').textContent = risk.score;
    document.getElementById('riskMeter').style.width = `${risk.score}%`;
    setList('riskFactors', risk.factors);
    document.getElementById('riskSuggestion').textContent = risk.suggestion;

    document.getElementById('adviceTitle').textContent = `${advice.deficiencyName} · 科学施肥方案`;
    document.getElementById('farmerAdvice').textContent = advice.farmerSimpleAdvice;
    document.getElementById('expertAdvice').textContent = advice.professionalAdvice;
    document.getElementById('adviceIntro').textContent = advice.introduction;
    setList('agronomicList', advice.agronomicMeasures);
    setList('organicList', advice.organicPlan);
    setList('chemicalList', advice.chemicalPlan);
    setList('safetyList', advice.safetyReminders);

    const evidence = [...new Set(data.candidates.flatMap((candidate) => candidate.evidence || []))];
    const tagContainer = document.getElementById('evidenceTags');
    tagContainer.innerHTML = '';
    (evidence.length ? evidence : ['语义相似度匹配']).forEach((item) => {
        const tag = document.createElement('span');
        tag.textContent = item;
        tagContainer.appendChild(tag);
    });
    document.getElementById('retrievalMeta').textContent =
        `${data.retrieval.strategy} · 知识条目 ${data.retrieval.knowledgeEntries} 条 · 命中术语 ${data.retrieval.matchedTerms} 个`;

    const sourceList = document.getElementById('sourceList');
    sourceList.innerHTML = '';
    (data.sources || []).forEach((source, index) => {
        const item = document.createElement('div');
        item.className = 'source-item';
        item.innerHTML = `
            <span class="source-index">${String(index + 1).padStart(2, '0')}</span>
            <div>
                <strong>${escapeHtml(source.title)}</strong>
                <small>${escapeHtml(source.publisher)}</small>
            </div>
        `;
        sourceList.appendChild(item);
    });

    document.getElementById('civicNote').textContent = data.civicNote;

    const expertPanel = document.querySelector('[data-panel="expert"]');
    const farmerPanel = document.querySelector('[data-panel="farmer"]');
    document.querySelectorAll('.tab-button').forEach((button) => {
        const active = button.dataset.tab === 'farmer';
        button.classList.toggle('active', active);
    });
    farmerPanel.hidden = false;
    expertPanel.hidden = true;

    results.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

document.querySelectorAll('.tab-button').forEach((button) => {
    button.addEventListener('click', () => {
        document.querySelectorAll('.tab-button').forEach((item) => item.classList.remove('active'));
        button.classList.add('active');
        document.querySelectorAll('[data-panel]').forEach((panel) => {
            panel.hidden = panel.dataset.panel !== button.dataset.tab;
        });
    });
});

async function loadKnowledgeSummary() {
    try {
        const response = await fetch('/api/knowledge');
        if (!response.ok) return;
        const data = await response.json();
        document.getElementById('kbVersion').textContent = `v${data.version}`;
        document.getElementById('heroEntryCount').textContent = data.entryCount;
    } catch (error) {
        document.getElementById('kbVersion').textContent = '本地内置';
    }
}

async function loadStats() {
    try {
        const response = await fetch('/api/stats');
        if (!response.ok) return;
        const data = await response.json();
        document.getElementById('statTotal').textContent = data.totalQueries;
        document.getElementById('statTop').textContent = data.topNutrient;
        document.getElementById('statLatency').textContent = data.avgRetrievalMs;
        document.getElementById('statRecent').textContent = data.samplePrompts?.[0] || '等待第一次诊断';
        document.getElementById('heroQueryCount').textContent = data.totalQueries;
    } catch (error) {
        // 统计看板不可用时不阻断主诊断链路。
    }
}

loadKnowledgeSummary();
loadStats();



