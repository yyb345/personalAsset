// --- 登录状态检查函数 ---
async function checkAuthStatus() {
    try {
        const response = await fetch('/api/auth/check');
        const data = await response.json();
        
        if (!data.authenticated) {
            // 未登录，跳转到登录页
            window.location.href = '/login.html';
            return false;
        }
        
        // 已登录，更新用户信息显示
        const userNameElement = document.getElementById('user-name');
        if (userNameElement && data.username) {
            userNameElement.textContent = data.fullName || data.username;
        }
        
        return true;
    } catch (error) {
        console.error('检查登录状态失败:', error);
        window.location.href = '/login.html';
        return false;
    }
}

document.addEventListener('DOMContentLoaded', function () {
    // --- 登录状态检查 ---
    checkAuthStatus();

    // --- DOM Elements ---
    const menu = document.querySelector('.menu');
    const menuItems = document.querySelectorAll('.menu-item');
    const pages = document.querySelectorAll('.page');
    const monthSelectorContainer = document.querySelector('.month-selector-container');
    
    // 移动端菜单元素
    const mobileMenuToggle = document.getElementById('mobile-menu-toggle');
    const sidebar = document.querySelector('.sidebar');
    const sidebarOverlay = document.getElementById('sidebar-overlay');
    
    // 用户相关元素
    const userNameElement = document.getElementById('user-name');
    const btnLogout = document.getElementById('btn-logout');
    
    // 登出按钮事件
    if (btnLogout) {
        btnLogout.addEventListener('click', async () => {
            if (confirm('确定要登出吗？')) {
                try {
                    const response = await fetch('/api/auth/logout', {
                        method: 'POST'
                    });
                    
                    if (response.ok) {
                        window.location.href = '/login.html';
                    } else {
                        alert('登出失败，请重试');
                    }
                } catch (error) {
                    console.error('登出错误:', error);
                    alert('登出失败，请重试');
                }
            }
        });
    }
    
    const assetForm = document.getElementById('asset-form');
    const assetsTableBody = document.querySelector('#assets-table tbody');
    const viewMonthInput = document.getElementById('view-month');
    const entryDateInput = document.getElementById('entryDate');

    const barChartContainer = document.getElementById('bar-chart-container');
    const lineChartContainer = document.getElementById('line-chart-container');
    const stackedBarChartContainer = document.getElementById('stacked-bar-chart-container');

    // Future assets page elements
    const baseMonthInput = document.getElementById('base-month');
    const loadBaseAssetsBtn = document.getElementById('load-base-assets');
    const currentAssetValueSpan = document.getElementById('current-asset-value');
    const incomeItemsContainer = document.getElementById('income-items-container');
    const expenseItemsContainer = document.getElementById('expense-items-container');
    const addIncomeItemBtn = document.getElementById('add-income-item');
    const addExpenseItemBtn = document.getElementById('add-expense-item');
    const targetMonthInput = document.getElementById('target-month');
    const calculateFutureBtn = document.getElementById('calculate-future');
    const futureResultSection = document.getElementById('future-result');
    
    let incomeItemIndex = 1;
    let expenseItemIndex = 1;

    // Edit modal elements
    const editModal = document.getElementById('edit-modal');
    const editAssetForm = document.getElementById('edit-asset-form');
    const editAssetId = document.getElementById('edit-asset-id');
    const editEntryDate = document.getElementById('edit-entryDate');
    const editName = document.getElementById('edit-name');
    const editType = document.getElementById('edit-type');
    const editValue = document.getElementById('edit-value');
    const editCurrency = document.getElementById('edit-currency');
    const closeModal = document.querySelector('.close');
    const btnCancel = document.querySelector('.btn-cancel');

    // --- ECharts Instances ---
    let barChart = null;
    let lineChart = null;
    let stackedBarChart = null;
    let babaChart = null;
    let orientChart = null;
    let koChart = null;
    const USD_TO_CNY_RATE = 5.4;
    const SGD_TO_CNY_RATE = 5.3; // 新币对人民币汇率

    // --- Formatters ---
    const cnyFormatter = new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' });
    const usdFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' });

    // --- Page Navigation ---
    const pageToHash = {
        'page-entry': '#entry',
        'page-charts': '#charts',
        'page-future': '#future',
        'page-stocks': '#stocks'
    };
    
    const hashToPage = {
        '#entry': 'page-entry',
        '#charts': 'page-charts',
        '#future': 'page-future',
        '#stocks': 'page-stocks'
    };

    function switchPage(pageId, updateHash = true) {
        pages.forEach(page => page.classList.remove('active'));
        document.getElementById(pageId).classList.add('active');

        menuItems.forEach(item => {
            item.classList.toggle('active', item.getAttribute('data-page') === pageId);
        });
        
        // Update URL hash
        if (updateHash && pageToHash[pageId]) {
            window.location.hash = pageToHash[pageId];
        }
        
        // Show/hide month selector based on page
        monthSelectorContainer.style.display = (pageId === 'page-charts' || pageId === 'page-stocks') ? 'none' : 'flex';

        if (pageId === 'page-charts') {
            renderStackedBarChart(); // Call the new stacked bar chart render function
        } else if (pageId === 'page-stocks') {
            loadStockData();
        }
    }

    // Handle hash changes (browser back/forward buttons)
    window.addEventListener('hashchange', function() {
        const hash = window.location.hash || '#entry';
        const pageId = hashToPage[hash] || 'page-entry';
        switchPage(pageId, false); // Don't update hash again to avoid loop
    });

    menu.addEventListener('click', function(event) {
        event.preventDefault();
        const menuItem = event.target.closest('.menu-item');
        if (menuItem) {
            switchPage(menuItem.getAttribute('data-page'));
            // 在移动端点击菜单后关闭侧边栏
            closeMobileSidebar();
        }
    });
    
    // --- 移动端菜单功能 ---
    function openMobileSidebar() {
        if (sidebar) sidebar.classList.add('active');
        if (sidebarOverlay) sidebarOverlay.classList.add('active');
        document.body.style.overflow = 'hidden'; // 防止背景滚动
    }
    
    function closeMobileSidebar() {
        if (sidebar) sidebar.classList.remove('active');
        if (sidebarOverlay) sidebarOverlay.classList.remove('active');
        document.body.style.overflow = ''; // 恢复滚动
    }
    
    // 汉堡菜单按钮点击
    if (mobileMenuToggle) {
        mobileMenuToggle.addEventListener('click', function() {
            if (sidebar.classList.contains('active')) {
                closeMobileSidebar();
            } else {
                openMobileSidebar();
            }
        });
    }
    
    // 点击遮罩层关闭侧边栏
    if (sidebarOverlay) {
        sidebarOverlay.addEventListener('click', closeMobileSidebar);
    }

    // --- Core Functions ---
    function initializeMonthInputs() {
        const now = new Date();
        const year = now.getFullYear();
        const month = (now.getMonth() + 1).toString().padStart(2, '0');
        const currentMonth = `${year}-${month}`;
        
        viewMonthInput.value = currentMonth;
        entryDateInput.value = currentMonth;
    }

    // Format month input as user types (auto-add hyphen)
    function formatMonthInput(input) {
        let value = input.value.replace(/\D/g, ''); // Remove non-digits
        if (value.length >= 4) {
            value = value.substring(0, 4) + '-' + value.substring(4, 6);
        }
        input.value = value;
    }

    // Validate month format (YYYY-MM)
    function validateMonthFormat(value) {
        const regex = /^\d{4}-(0[1-9]|1[0-2])$/;
        return regex.test(value);
    }

    // Add input formatting for month fields
    [viewMonthInput, entryDateInput, editEntryDate, baseMonthInput, targetMonthInput].forEach(input => {
        if (input) {
        input.addEventListener('input', function() {
            formatMonthInput(this);
        });
        
        input.addEventListener('blur', function() {
            if (this.value && !validateMonthFormat(this.value)) {
                alert('请输入正确的月份格式: YYYY-MM (例如: 2025-11)');
                this.focus();
            }
        });
        }
    });

    async function loadAssets(month) {
        if (!month) {
            assetsTableBody.innerHTML = '<tr><td colspan="6">请选择一个有效的月份</td></tr>';
            return;
        }
        try {
            const response = await fetch(`/api/assets?month=${month}`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            const assets = await response.json();
            assetsTableBody.innerHTML = '';
            if (assets.length === 0) {
                assetsTableBody.innerHTML = `<tr><td colspan="6">该月份（${month}）没有资产记录</td></tr>`;
            } else {
                assets.forEach(asset => {
                    const row = document.createElement('tr');
                    const formattedValue = asset.currency && asset.currency.toLowerCase() === 'usd'
                        ? usdFormatter.format(asset.value)
                        : cnyFormatter.format(asset.value);
                    
                    // Extract YYYY-MM from entryDate (which is YYYY-MM-DD)
                    const monthOnly = asset.entryDate.substring(0, 7);

                    row.innerHTML = `
                        <td data-label="名称">${escapeHTML(asset.name)}</td>
                        <td data-label="类型">${escapeHTML(asset.type)}</td>
                        <td data-label="价值">${formattedValue}</td>
                        <td data-label="币种">${escapeHTML(asset.currency)}</td>
                        <td data-label="录入日期">${asset.entryDate}</td>
                        <td data-label="操作">
                            <button class="edit-btn" 
                                data-id="${asset.id}"
                                data-name="${escapeHTML(asset.name)}"
                                data-type="${escapeHTML(asset.type)}"
                                data-value="${asset.value}"
                                data-currency="${escapeHTML(asset.currency)}"
                                data-date="${monthOnly}">编辑</button>
                            <button class="delete-btn" data-id="${asset.id}">删除</button>
                        </td>
                    `;
                    assetsTableBody.appendChild(row);
                });
            }
        } catch (error) {
            console.error("无法加载资产:", error);
            assetsTableBody.innerHTML = '<tr><td colspan="6">加载资产失败</td></tr>';
        }
    }

    // --- Future Assets Functions ---
    let currentBaseAssets = 0;

    function addIncomeItem() {
        const itemHtml = `
            <div class="income-item" data-index="${incomeItemIndex}">
                <div class="item-row">
                    <input type="text" class="item-name" placeholder="收入名称（如：工资）" required>
                    <input type="number" class="item-amount" step="0.01" placeholder="金额" required>
                    <select class="item-currency">
                        <option value="CNY" selected>CNY</option>
                        <option value="SGD">SGD</option>
                    </select>
                    <button type="button" class="btn-remove-item" onclick="removeIncomeItem(${incomeItemIndex})" title="删除">×</button>
                </div>
            </div>
        `;
        incomeItemsContainer.insertAdjacentHTML('beforeend', itemHtml);
        incomeItemIndex++;
    }

    function addExpenseItem() {
        const itemHtml = `
            <div class="expense-item" data-index="${expenseItemIndex}">
                <div class="item-row">
                    <input type="text" class="item-name" placeholder="支出名称（如：房租）" required>
                    <input type="number" class="item-amount" step="0.01" placeholder="金额" required>
                    <select class="item-currency">
                        <option value="CNY" selected>CNY</option>
                        <option value="SGD">SGD</option>
                    </select>
                    <button type="button" class="btn-remove-item" onclick="removeExpenseItem(${expenseItemIndex})" title="删除">×</button>
                </div>
            </div>
        `;
        expenseItemsContainer.insertAdjacentHTML('beforeend', itemHtml);
        expenseItemIndex++;
    }

    window.removeIncomeItem = function(index) {
        const items = incomeItemsContainer.querySelectorAll('.income-item');
        if (items.length > 1) {
            const item = incomeItemsContainer.querySelector(`.income-item[data-index="${index}"]`);
            if (item) item.remove();
        } else {
            alert('至少保留一个收入项目');
        }
    }

    window.removeExpenseItem = function(index) {
        const items = expenseItemsContainer.querySelectorAll('.expense-item');
        if (items.length > 1) {
            const item = expenseItemsContainer.querySelector(`.expense-item[data-index="${index}"]`);
            if (item) item.remove();
        } else {
            alert('至少保留一个支出项目');
        }
    }

    function getIncomeItems() {
        const items = [];
        const incomeElements = incomeItemsContainer.querySelectorAll('.income-item');
        incomeElements.forEach(element => {
            const name = element.querySelector('.item-name').value.trim();
            const amount = parseFloat(element.querySelector('.item-amount').value);
            const currency = element.querySelector('.item-currency').value;
            if (name && !isNaN(amount) && amount >= 0) {
                items.push({ name, amount, currency });
            }
        });
        return items;
    }

    function getExpenseItems() {
        const items = [];
        const expenseElements = expenseItemsContainer.querySelectorAll('.expense-item');
        expenseElements.forEach(element => {
            const name = element.querySelector('.item-name').value.trim();
            const amount = parseFloat(element.querySelector('.item-amount').value);
            const currency = element.querySelector('.item-currency').value;
            if (name && !isNaN(amount) && amount >= 0) {
                items.push({ name, amount, currency });
            }
        });
        return items;
    }

    async function loadBaseAssets() {
        const baseMonth = baseMonthInput.value;
        if (!baseMonth || !validateMonthFormat(baseMonth)) {
            alert('请输入有效的基准月份');
            return;
        }

        try {
            const response = await fetch(`/api/report?month=${baseMonth}`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            const report = await response.json();
            currentBaseAssets = parseFloat(report.totalValue) || 0;
            currentAssetValueSpan.textContent = cnyFormatter.format(currentBaseAssets);
            currentAssetValueSpan.style.color = '#27ae60';
            currentAssetValueSpan.style.fontWeight = 'bold';
        } catch (error) {
            console.error("无法加载资产:", error);
            alert("加载资产失败，请确认该月份有资产数据");
            currentAssetValueSpan.textContent = '--';
        }
    }

    function calculateMonthsDifference(startMonth, endMonth) {
        const [startYear, startMon] = startMonth.split('-').map(Number);
        const [endYear, endMon] = endMonth.split('-').map(Number);
        return (endYear - startYear) * 12 + (endMon - startMon);
    }

    function convertToCNY(amount, currency) {
        if (!amount || isNaN(amount)) return 0;
        if (currency === 'SGD') {
            return amount * SGD_TO_CNY_RATE;
        }
        return amount; // CNY不需要转换
    }

    function formatCurrency(amount, currency) {
        if (currency === 'SGD') {
            return new Intl.NumberFormat('en-SG', { style: 'currency', currency: 'SGD' }).format(amount);
        }
        return cnyFormatter.format(amount);
    }

    function calculateFutureAssets() {
        const baseMonth = baseMonthInput.value;
        const targetMonth = targetMonthInput.value;

        // Validation
        if (!baseMonth || !validateMonthFormat(baseMonth)) {
            alert('请输入有效的基准月份');
            return;
        }

        if (currentBaseAssets === 0) {
            alert('请先加载当前资产');
            return;
        }

        const incomeItems = getIncomeItems();
        const expenseItems = getExpenseItems();

        if (incomeItems.length === 0) {
            alert('请至少添加一个收入项目');
            return;
        }

        if (expenseItems.length === 0) {
            alert('请至少添加一个支出项目');
            return;
        }

        if (!targetMonth || !validateMonthFormat(targetMonth)) {
            alert('请输入有效的截至月份');
            return;
        }

        const monthsDiff = calculateMonthsDifference(baseMonth, targetMonth);
        
        if (monthsDiff <= 0) {
            alert('截至月份必须晚于基准月份');
            return;
        }

        // Calculate total income in CNY
        let totalIncomeInCNY = 0;
        const incomeDetails = incomeItems.map(item => {
            const amountInCNY = convertToCNY(item.amount, item.currency);
            totalIncomeInCNY += amountInCNY;
            return `${item.name}: ${formatCurrency(item.amount, item.currency)}`;
        });

        // Calculate total expense in CNY
        let totalExpenseInCNY = 0;
        const expenseDetails = expenseItems.map(item => {
            const amountInCNY = convertToCNY(item.amount, item.currency);
            totalExpenseInCNY += amountInCNY;
            return `${item.name}: ${formatCurrency(item.amount, item.currency)}`;
        });
        
        // Calculate future assets
        const netMonthlyIncome = totalIncomeInCNY - totalExpenseInCNY;
        const futureAssets = currentBaseAssets + (netMonthlyIncome * monthsDiff);
        const growth = futureAssets - currentBaseAssets;
        const growthPercentage = ((growth / currentBaseAssets) * 100).toFixed(2);

        // Display results
        document.getElementById('result-base-month').textContent = baseMonth;
        document.getElementById('result-current-assets').textContent = cnyFormatter.format(currentBaseAssets);
        document.getElementById('result-target-month').textContent = targetMonth;
        document.getElementById('result-months').textContent = `${monthsDiff} 个月`;
        
        // Show total income with breakdown
        document.getElementById('result-total-income').innerHTML = `${cnyFormatter.format(totalIncomeInCNY)}<br><small>${incomeDetails.join('<br>')}</small>`;
        
        // Show total expense with breakdown
        document.getElementById('result-total-expense').innerHTML = `${cnyFormatter.format(totalExpenseInCNY)}<br><small>${expenseDetails.join('<br>')}</small>`;
        
        // Show net income
        document.getElementById('result-net-income').textContent = cnyFormatter.format(netMonthlyIncome);
        document.getElementById('result-net-income').style.color = netMonthlyIncome >= 0 ? '#ffffff' : '#ffcccc';
        
        document.getElementById('result-future-assets').textContent = cnyFormatter.format(futureAssets);
        
        const growthText = `${cnyFormatter.format(growth)} (${growthPercentage}%)`;
        const growthElement = document.getElementById('result-growth');
        growthElement.textContent = growthText;
        growthElement.style.color = growth >= 0 ? '#27ae60' : '#e74c3c';

        futureResultSection.style.display = 'block';
        
        // Smooth scroll to results
        futureResultSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    // --- Stock Analysis Functions ---
    let currentTimeRange = '1M';

    // Stock symbols configuration
    const stocks = {
        'baba': { symbol: 'BABA', name: '阿里巴巴' },
        'orient': { symbol: '1797.HK', name: '东方甄选' },
        'ko': { symbol: 'KO', name: '可口可乐' }
    };

    async function fetchStockData(symbol, range) {
        try {
            // 计算时间范围
            const endDate = new Date();
            const startDate = new Date();
            
            switch(range) {
                case '1M':
                    startDate.setMonth(startDate.getMonth() - 1);
                    break;
                case '3M':
                    startDate.setMonth(startDate.getMonth() - 3);
                    break;
                case '6M':
                    startDate.setMonth(startDate.getMonth() - 6);
                    break;
                case '1Y':
                    startDate.setFullYear(startDate.getFullYear() - 1);
                    break;
            }

            const period1 = Math.floor(startDate.getTime() / 1000);
            const period2 = Math.floor(endDate.getTime() / 1000);

            // 使用我们的后端代理API
            const url = `/api/stocks/data?symbol=${symbol}&period1=${period1}&period2=${period2}`;
            
            const response = await fetch(url);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            const data = await response.json();
            
            if (data.chart && data.chart.result && data.chart.result.length > 0) {
                const result = data.chart.result[0];
                const timestamps = result.timestamp;
                const quotes = result.indicators.quote[0];
                
                return {
                    dates: timestamps.map(ts => new Date(ts * 1000).toLocaleDateString('zh-CN')),
                    prices: quotes.close,
                    currentPrice: quotes.close[quotes.close.length - 1],
                    previousPrice: quotes.close[0],
                    high: quotes.high,
                    low: quotes.low,
                    open: quotes.open,
                    volume: quotes.volume
                };
            }
            
            return null;
        } catch (error) {
            console.error(`获取股票 ${symbol} 数据失败:`, error);
            return null;
        }
    }

    function renderStockChart(containerId, stockData, stockName) {
        const container = document.getElementById(containerId);
        if (!container) return;

        let chart;
        if (containerId === 'baba-chart') {
            if (!babaChart) babaChart = echarts.init(container);
            chart = babaChart;
        } else if (containerId === 'orient-chart') {
            if (!orientChart) orientChart = echarts.init(container);
            chart = orientChart;
        } else if (containerId === 'ko-chart') {
            if (!koChart) koChart = echarts.init(container);
            chart = koChart;
        }

        if (!chart) return;

        const option = {
            title: {
                text: '',
                left: 'center'
            },
            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'cross'
                },
                formatter: function(params) {
                    const param = params[0];
                    return `${param.name}<br/>收盘: $${param.value.toFixed(2)}`;
                }
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '10%',
                top: '10%',
                containLabel: true
            },
            xAxis: {
                type: 'category',
                data: stockData.dates,
                axisLabel: {
                    rotate: 45,
                    interval: Math.floor(stockData.dates.length / 6)
                }
            },
            yAxis: {
                type: 'value',
                scale: true,
                axisLabel: {
                    formatter: '${value}'
                }
            },
            series: [{
                name: stockName,
                type: 'line',
                data: stockData.prices,
                smooth: true,
                lineStyle: {
                    width: 2
                },
                areaStyle: {
                    color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
                        offset: 0,
                        color: 'rgba(58, 77, 233, 0.3)'
                    }, {
                        offset: 1,
                        color: 'rgba(58, 77, 233, 0.05)'
                    }])
                }
            }]
        };

        chart.setOption(option);
    }

    function updateStockInfo(stockId, stockData) {
        const priceElement = document.getElementById(`${stockId}-price`);
        const changeElement = document.getElementById(`${stockId}-change`);
        
        if (stockData && priceElement && changeElement) {
            const currentPrice = stockData.currentPrice;
            const previousPrice = stockData.previousPrice;
            const change = currentPrice - previousPrice;
            const changePercent = ((change / previousPrice) * 100).toFixed(2);
            
            priceElement.textContent = `$${currentPrice.toFixed(2)}`;
            
            const changeText = `${change >= 0 ? '+' : ''}$${change.toFixed(2)} (${changePercent}%)`;
            changeElement.textContent = changeText;
            changeElement.style.color = change >= 0 ? '#27ae60' : '#e74c3c';
            changeElement.classList.toggle('positive', change >= 0);
            changeElement.classList.toggle('negative', change < 0);
        }
    }

    async function loadStockData() {
        // Show loading indicators
        ['baba', 'orient', 'ko'].forEach(id => {
            const priceEl = document.getElementById(`${id}-price`);
            const changeEl = document.getElementById(`${id}-change`);
            if (priceEl) priceEl.textContent = '加载中...';
            if (changeEl) changeEl.textContent = '';
        });

        try {
            // Load BABA
            const babaData = await fetchStockData(stocks.baba.symbol, currentTimeRange);
            if (babaData) {
                renderStockChart('baba-chart', babaData, stocks.baba.name);
                updateStockInfo('baba', babaData);
            }

            // Load Orient
            const orientData = await fetchStockData(stocks.orient.symbol, currentTimeRange);
            if (orientData) {
                renderStockChart('orient-chart', orientData, stocks.orient.name);
                updateStockInfo('orient', orientData);
            }

            // Load KO
            const koData = await fetchStockData(stocks.ko.symbol, currentTimeRange);
            if (koData) {
                renderStockChart('ko-chart', koData, stocks.ko.name);
                updateStockInfo('ko', koData);
            }
        } catch (error) {
            console.error('加载股票数据失败:', error);
            alert('股票数据加载失败，显示模拟数据供演示。请稍后重试或刷新页面。');
        }
    }

    // --- Charting Functions ---
    async function renderStackedBarChart() {
        try {
            const response = await fetch('/api/reports/stacked-bar-data');
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const dataset = await response.json(); // dataset is List<List<Object>>

            if (dataset.length <= 1) { // Only header row means no data
                stackedBarChartContainer.innerHTML = '<p>没有足够的数据来生成图表。</p>';
                if(stackedBarChart) stackedBarChart.clear();
                return;
            }

            // Extract months (dimensions) and asset types (series names)
            const header = dataset[0];
            const months = header.slice(1); // ['2025-10', '2025-11', ...]
            const assetTypes = dataset.slice(1).map(row => row[0]); // ['现金', '股票', ...]

            if (!stackedBarChart) {
                stackedBarChart = echarts.init(stackedBarChartContainer);
            }

            // Create series with data directly mapped from dataset
            const series = assetTypes.map((type, index) => {
                const rowData = dataset[index + 1]; // Get the row for this asset type
                const values = rowData.slice(1); // Remove the first element (asset type name)
                
                return {
                    name: type,
                    type: 'bar',
                    stack: 'total', // This makes it a stacked bar chart
                    emphasis: { focus: 'series' },
                    data: values
                };
            });

            const option = {
                title: { text: '月度资产构成分析 (CNY)', left: 'center' },
                tooltip: {
                    trigger: 'axis',
                    axisPointer: { type: 'shadow' },
                    formatter: function (params) {
                        let total = 0;
                        let res = `<strong>${params[0].name}</strong><br/>`;
                        params.forEach(function (item) {
                            total += item.value;
                            res += `${item.marker} ${item.seriesName}: ${cnyFormatter.format(item.value)}<br/>`;
                        });
                        res += `<hr style="margin: 5px 0; border-top: 1px solid #ccc;"/><strong>总计: ${cnyFormatter.format(total)}</strong>`;
                        return res;
                    }
                },
                legend: {
                    data: assetTypes,
                    bottom: 0,
                    type: 'scroll'
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '15%', // Adjust for legend
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    data: months
                },
                yAxis: {
                    type: 'value',
                    axisLabel: { formatter: '¥{value}' }
                },
                series: series
            };
            stackedBarChart.setOption(option);

        } catch (error) {
            console.error('无法渲染堆叠柱状图:', error);
            stackedBarChartContainer.innerHTML = '<p>加载图表数据失败。</p>';
        }
    }

    // --- Event Listeners ---
    viewMonthInput.addEventListener('change', async () => {
        await loadAssets(viewMonthInput.value);
    });

    // Future assets event listeners
    if (loadBaseAssetsBtn) {
        loadBaseAssetsBtn.addEventListener('click', loadBaseAssets);
    }

    if (addIncomeItemBtn) {
        addIncomeItemBtn.addEventListener('click', addIncomeItem);
    }

    if (addExpenseItemBtn) {
        addExpenseItemBtn.addEventListener('click', addExpenseItem);
    }

    if (calculateFutureBtn) {
        calculateFutureBtn.addEventListener('click', calculateFutureAssets);
    }

    // Stock analysis event listeners
    const timeButtons = document.querySelectorAll('.time-btn');
    timeButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            timeButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentTimeRange = this.getAttribute('data-range');
            loadStockData();
        });
    });

    const refreshStocksBtn = document.getElementById('refresh-stocks');
    if (refreshStocksBtn) {
        refreshStocksBtn.addEventListener('click', loadStockData);
    }

    assetForm.addEventListener('submit', async function (event) {
        event.preventDefault();
        const formData = new FormData(assetForm);
        const monthValue = formData.get('entryDate');

        const assetData = {
            name: formData.get('name'),
            type: formData.get('type'),
            value: parseFloat(formData.get('value')),
            currency: formData.get('currency'),
            entryDate: `${monthValue}-01` 
        };

        try {
            const response = await fetch('/api/assets', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(assetData),
            });
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            assetForm.reset();
            initializeMonthInputs();
            
            viewMonthInput.value = monthValue;
            await loadAssets(monthValue);
            
        } catch (error) {
            console.error("无法添加资产:", error);
            alert("添加资产失败，请查看控制台日志。");
        }
    });

    assetsTableBody.addEventListener('click', async function(event) {
        // Handle edit button click
        if (event.target.classList.contains('edit-btn')) {
            const button = event.target;
            editAssetId.value = button.getAttribute('data-id');
            editName.value = button.getAttribute('data-name');
            editType.value = button.getAttribute('data-type');
            editValue.value = button.getAttribute('data-value');
            editCurrency.value = button.getAttribute('data-currency');
            editEntryDate.value = button.getAttribute('data-date');
            
            editModal.classList.add('show');
        }
        
        // Handle delete button click
        if (event.target.classList.contains('delete-btn')) {
            const assetId = event.target.getAttribute('data-id');
            if (confirm('您确定要删除这条资产记录吗？')) {
                try {
                    const response = await fetch(`/api/assets/${assetId}`, { method: 'DELETE' });
                    if (response.ok || response.status === 204) {
                        await loadAssets(viewMonthInput.value);
                    } else {
                        throw new Error(`删除失败! Status: ${response.status}`);
                    }
                } catch (error) {
                    console.error("删除资产时出错:", error);
                    alert("删除资产失败，请查看控制台日志。");
                }
            }
        }
    });

    // --- Modal Event Handlers ---
    // Close modal when clicking X button
    closeModal.addEventListener('click', function() {
        editModal.classList.remove('show');
    });

    // Close modal when clicking Cancel button
    btnCancel.addEventListener('click', function() {
        editModal.classList.remove('show');
    });

    // Close modal when clicking outside the modal content
    window.addEventListener('click', function(event) {
        if (event.target === editModal) {
            editModal.classList.remove('show');
        }
    });

    // Handle edit form submission
    editAssetForm.addEventListener('submit', async function(event) {
        event.preventDefault();
        
        const assetId = editAssetId.value;
        const formData = new FormData(editAssetForm);
        const monthValue = formData.get('entryDate');

        const assetData = {
            name: formData.get('name'),
            type: formData.get('type'),
            value: parseFloat(formData.get('value')),
            currency: formData.get('currency'),
            entryDate: `${monthValue}-01`
        };

        try {
            const response = await fetch(`/api/assets/${assetId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(assetData),
            });
            
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            // Close modal and reload assets
            editModal.classList.remove('show');
            await loadAssets(viewMonthInput.value);
            
        } catch (error) {
            console.error("无法更新资产:", error);
            alert("更新资产失败，请查看控制台日志。");
        }
    });

    // --- Utility & Initialization ---
    function escapeHTML(str) {
        if (typeof str !== 'string') return str;
        return str.replace(/[&<>"']/g, match => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'})[match]);
    }

    async function init() {
        initializeMonthInputs();
        await loadAssets(viewMonthInput.value);
        
        // Initialize future assets page inputs
        if (baseMonthInput) {
            const now = new Date();
            const year = now.getFullYear();
            const month = (now.getMonth() + 1).toString().padStart(2, '0');
            baseMonthInput.value = `${year}-${month}`;
            
            // Set target month to one year from now
            const nextYear = year + 1;
            if (targetMonthInput) {
                targetMonthInput.value = `${nextYear}-${month}`;
            }
        }
        
        // Check URL hash and navigate to the corresponding page
        const hash = window.location.hash || '#entry';
        const pageId = hashToPage[hash] || 'page-entry';
        switchPage(pageId, false); // Don't update hash since we're reading from it
    }

    init();
});
