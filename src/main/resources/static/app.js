const cryptoList = ['bitcoin', 'ethereum', 'binancecoin', 'solana', 'cardano', 'polkadot'];
const symbolsMap = { 'bitcoin': 'BTC', 'ethereum': 'ETH', 'binancecoin': 'BNB', 'solana': 'SOL', 'cardano': 'ADA', 'polkadot': 'DOT' };
const staticPrices = { 'ethereum': 2267.70, 'binancecoin': 759.94, 'solana': 98.94, 'cardano': 0.297, 'polkadot': 1.52 };
let currentBtcPrice = 76394.00;

function getWallet() {
    const initialWallet = { usd: 1000000, bitcoin: 0, ethereum: 0, binancecoin: 0, solana: 0, cardano: 0, polkadot: 0 };
    return JSON.parse(localStorage.getItem('crypto_broker_wallet')) || initialWallet;
}

function updateHeader() {
    const wallet = getWallet();
    const element = document.getElementById('wallet-usd-header');
    if (element) {
        element.innerText = `$${wallet.usd.toLocaleString(undefined, {minimumFractionDigits: 2})}`;
    }
}

async function initTickers() {
    try {
        const res = await fetch(`https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd`);
        if (res.ok) {
            const data = await res.json();
            if (data.bitcoin) currentBtcPrice = data.bitcoin.usd;
        }
    } catch (err) { console.error("API error", err); }

    const container = document.getElementById('ticker-container');
    if (!container) return;

    container.innerHTML = '';
    cryptoList.forEach(id => {
        const price = (id === 'bitcoin') ? currentBtcPrice : staticPrices[id];
        container.innerHTML += `<div class="ticker-card d-flex justify-content-between align-items-center">
            <div><span class="symbol-badge">${symbolsMap[id]}</span></div>
            <div class="text-end"><span class="price-text">$${price.toLocaleString(undefined, {minimumFractionDigits: 2})}</span></div>
        </div>`;
    });
    const lastUpdateEl = document.getElementById('last-update');
    if (lastUpdateEl) {
        lastUpdateEl.innerText = `Ostatnia aktualizacja: ${new Date().toLocaleTimeString()}`;
    }
}

const strategySelect = document.getElementById('orderStrategy');
if (strategySelect) {
    strategySelect.addEventListener('change', function() {
        const priceInput = document.getElementById('targetPrice');
        if (this.value === 'PKC') {
            priceInput.disabled = true; priceInput.value = ""; priceInput.required = false;
        } else {
            priceInput.disabled = false; priceInput.required = true;
        }
    });
}

const orderForm = document.getElementById('orderForm');
if (orderForm) {
    orderForm.onsubmit = async (e) => {
        e.preventDefault();
        const cryptoId = document.getElementById('cryptoId').value;
        const type = document.getElementById('type').value;
        const amount = parseFloat(document.getElementById('amount').value);
        const strategy = document.getElementById('orderStrategy').value;
        const priceToUse = strategy === 'PKC' ? (cryptoId === 'bitcoin' ? currentBtcPrice : staticPrices[cryptoId]) : parseFloat(document.getElementById('targetPrice').value);

        try {
            const response = await fetch('/api/orders/start', {
                method: 'POST',
                headers: {'Content-Type': 'application/json'},
                body: JSON.stringify({
                    cryptoId, type, orderStrategy: strategy,
                    clientTier: document.getElementById('clientTier').value,
                    targetPrice: priceToUse, amount,
                    startDate: new Date(document.getElementById('startDate').value).toISOString(),
                    expiryDate: new Date(document.getElementById('expiryDate').value).toISOString()
                })
            });
            if (response.ok) {
                const data = await response.json();
                document.getElementById('response').innerHTML = `
                    <div class="alert alert-info py-3 shadow-sm">
                        <div class="spinner-border spinner-border-sm text-info me-2"></div>
                        Zlecenie <strong>${data.transactionId}</strong> przyjęte. Czekam na finalizację...
                    </div>`;
                startPolling(data.transactionId);
            }
        } catch (error) {
            document.getElementById('response').innerHTML = `<div class="alert alert-danger">Błąd połączenia z serwerem.</div>`;
        }
    };
}

function startPolling(transactionId) {
    const pollInterval = setInterval(async () => {
        try {
            const res = await fetch(`/api/orders/status/${transactionId}`);
            if (res.ok) {
                const result = await res.json();
                if (result.status === "SUCCESS") {
                    clearInterval(pollInterval);
                    applyTransactionToWallet(result, transactionId);
                }
            }
        } catch (err) {
            console.error("Błąd odpytywania o status:", err);
        }
    }, 2000);
}

function applyTransactionToWallet(data, originalId) {
    const wallet = getWallet();
    const tId = data.transactionId || originalId;

    const amount = parseFloat(data.amount || 0);
    const totalCost = parseFloat(data.totalCost || data.transactionAmount || 0);
    const commission = parseFloat(data.commission || 0);

    if (data.type === 'BUY') {
        wallet.usd -= totalCost;
        wallet[data.cryptoId] = (wallet[data.cryptoId] || 0) + amount;
    } else {
        wallet.usd += totalCost;
        wallet[data.cryptoId] = (wallet[data.cryptoId] || 0) - amount;
    }

    localStorage.setItem('crypto_broker_wallet', JSON.stringify(wallet));
    updateHeader();

    let badgeColor = 'bg-secondary';
    if (data.clientTier === 'VIP') badgeColor = 'bg-warning text-dark';
    else if (data.clientTier === 'GOLD') badgeColor = 'bg-primary';

    // ZMIANA: Link teraz kieruje do /api/orders/download-pdf/${tId}
    document.getElementById('response').innerHTML = `
        <div class="alert alert-success shadow-lg text-start">
            <h5 class="alert-heading fw-bold">✅ Zlecenie sfinalizowane!</h5>
            <hr>
            <div class="mb-2">Poziom klienta: <span class="badge ${badgeColor}">${data.clientTier || 'Standard'}</span></div>
            <p class="mb-1">Aktywo: <strong>${amount} ${(data.cryptoId || '').toUpperCase()}</strong></p>
            <p class="mb-1 text-danger">Prowizja: -$${commission.toFixed(2)}</p>
            <p class="mb-2 fw-bold border-top pt-2">Łączny koszt: $${totalCost.toFixed(2)}</p>
    
            <a href="/api/orders/download-pdf/${tId}" target="_blank" class="btn btn-outline-success btn-sm w-100 fw-bold mt-2">
                📥 OTWÓRZ POTWIERDZENIE (PDF)
            </a>
            <small class="text-muted d-block mt-1 text-center" style="font-size: 0.7rem;">ID: ${tId}</small>
        </div>`;
}

initTickers();
updateHeader();

const startDateInput = document.getElementById('startDate');
const expiryDateInput = document.getElementById('expiryDate');
if (startDateInput && expiryDateInput) {
    const nowLocal = new Date(new Date().getTime() - new Date().getTimezoneOffset() * 60000);
    startDateInput.value = nowLocal.toISOString().slice(0, 16);
    expiryDateInput.value = new Date(nowLocal.getTime() + 3600000).toISOString().slice(0, 16);
}