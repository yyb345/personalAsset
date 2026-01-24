<template>
  <div class="stocks-page">
    <div class="stocks-container">
      <h2>股票分析</h2>
      
      <div class="stocks-control-panel">
        <div class="time-range-selector">
          <label>时间范围:</label>
          <button 
            v-for="range in timeRanges" 
            :key="range.value"
            class="time-btn" 
            :class="{ active: currentTimeRange === range.value }"
            @click="changeTimeRange(range.value)"
          >
            {{ range.label }}
          </button>
        </div>
        <button @click="refreshStocks" class="btn-refresh">🔄 刷新数据</button>
      </div>

      <div class="stocks-grid">
        <div v-for="stock in stocks" :key="stock.id" class="stock-card">
          <div class="stock-header">
            <h3>{{ stock.name }} ({{ stock.symbol }})</h3>
            <div class="stock-info">
              <span class="current-price">{{ stock.currentPrice }}</span>
              <span 
                class="price-change" 
                :class="{ positive: stock.changePositive, negative: !stock.changePositive }"
                :style="{ color: stock.changeColor }"
              >
                {{ stock.change }}
              </span>
            </div>
          </div>
          <div :ref="el => setChartRef(stock.id, el)" class="stock-chart"></div>
        </div>
      </div>

      <div class="stocks-disclaimer">
        <small>⚠️ 数据仅供参考，不构成投资建议。股票价格存在延迟，请以实际交易平台为准。</small>
        <small style="display: block; margin-top: 5px;">💡 如遇API限制，系统将自动使用模拟数据进行演示。数据每15分钟更新一次。</small>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import axios from '@/utils/axios'

const currentTimeRange = ref('1M')
const chartRefs = ref({})
const chartInstances = ref({})

const timeRanges = [
  { label: '1个月', value: '1M' },
  { label: '3个月', value: '3M' },
  { label: '6个月', value: '6M' },
  { label: '1年', value: '1Y' }
]

const stocks = ref([
  {
    id: 'baba',
    symbol: 'BABA',
    name: '阿里巴巴',
    currentPrice: '--',
    change: '--',
    changePositive: true,
    changeColor: '#27ae60'
  },
  {
    id: 'orient',
    symbol: '01797.HK',
    name: '东方甄选',
    currentPrice: '--',
    change: '--',
    changePositive: true,
    changeColor: '#27ae60'
  },
  {
    id: 'ko',
    symbol: 'KO',
    name: '可口可乐',
    currentPrice: '--',
    change: '--',
    changePositive: true,
    changeColor: '#27ae60'
  }
])

onMounted(() => {
  loadStockData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  Object.values(chartInstances.value).forEach(chart => {
    if (chart) chart.dispose()
  })
  window.removeEventListener('resize', handleResize)
})

const setChartRef = (id, el) => {
  if (el) {
    chartRefs.value[id] = el
  }
}

const handleResize = () => {
  Object.values(chartInstances.value).forEach(chart => {
    if (chart) chart.resize()
  })
}

const changeTimeRange = (range) => {
  currentTimeRange.value = range
  loadStockData()
}

const refreshStocks = () => {
  loadStockData()
}

const fetchStockData = async (symbol, range) => {
  try {
    const endDate = new Date()
    const startDate = new Date()
    
    switch(range) {
      case '1M':
        startDate.setMonth(startDate.getMonth() - 1)
        break
      case '3M':
        startDate.setMonth(startDate.getMonth() - 3)
        break
      case '6M':
        startDate.setMonth(startDate.getMonth() - 6)
        break
      case '1Y':
        startDate.setFullYear(startDate.getFullYear() - 1)
        break
    }

    const period1 = Math.floor(startDate.getTime() / 1000)
    const period2 = Math.floor(endDate.getTime() / 1000)

    const url = `/api/stocks/data?symbol=${symbol}&period1=${period1}&period2=${period2}`
    const response = await axios.get(url)
    
    if (response.data.chart && response.data.chart.result && response.data.chart.result.length > 0) {
      const result = response.data.chart.result[0]
      const timestamps = result.timestamp
      const quotes = result.indicators.quote[0]
      
      return {
        dates: timestamps.map(ts => new Date(ts * 1000).toLocaleDateString('zh-CN')),
        prices: quotes.close,
        currentPrice: quotes.close[quotes.close.length - 1],
        previousPrice: quotes.close[0],
        high: quotes.high,
        low: quotes.low,
        open: quotes.open,
        volume: quotes.volume
      }
    }
    
    return null
  } catch (error) {
    console.error(`获取股票 ${symbol} 数据失败:`, error)
    return null
  }
}

const renderStockChart = (stockId, stockData, stockName) => {
  const container = chartRefs.value[stockId]
  if (!container) return

  if (!chartInstances.value[stockId]) {
    chartInstances.value[stockId] = echarts.init(container)
  }

  const chart = chartInstances.value[stockId]

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
        const param = params[0]
        return `${param.name}<br/>收盘: $${param.value.toFixed(2)}`
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
  }

  chart.setOption(option)
}

const updateStockInfo = (stockId, stockData) => {
  const stock = stocks.value.find(s => s.id === stockId)
  if (stock && stockData) {
    const currentPrice = stockData.currentPrice
    const previousPrice = stockData.previousPrice
    const change = currentPrice - previousPrice
    const changePercent = ((change / previousPrice) * 100).toFixed(2)
    
    stock.currentPrice = `$${currentPrice.toFixed(2)}`
    stock.change = `${change >= 0 ? '+' : ''}$${change.toFixed(2)} (${changePercent}%)`
    stock.changePositive = change >= 0
    stock.changeColor = change >= 0 ? '#27ae60' : '#e74c3c'
  }
}

const loadStockData = async () => {
  // 显示加载状态
  stocks.value.forEach(stock => {
    stock.currentPrice = '加载中...'
    stock.change = ''
  })

  try {
    // 加载BABA
    const babaData = await fetchStockData('BABA', currentTimeRange.value)
    if (babaData) {
      renderStockChart('baba', babaData, '阿里巴巴')
      updateStockInfo('baba', babaData)
    }

    // 加载东方甄选
    const orientData = await fetchStockData('1797.HK', currentTimeRange.value)
    if (orientData) {
      renderStockChart('orient', orientData, '东方甄选')
      updateStockInfo('orient', orientData)
    }

    // 加载KO
    const koData = await fetchStockData('KO', currentTimeRange.value)
    if (koData) {
      renderStockChart('ko', koData, '可口可乐')
      updateStockInfo('ko', koData)
    }
  } catch (error) {
    console.error('加载股票数据失败:', error)
  }
}
</script>

<style scoped src="@/assets/styles/stocks.css"></style>


