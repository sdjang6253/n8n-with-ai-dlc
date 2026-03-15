import React from 'react'

const TOOLS = [
  {
    name: 'Grafana',
    desc: '메트릭 대시보드 (admin / admin)',
    url: 'http://localhost:13001',
    color: '#F46800',
    icon: '📊',
  },
  {
    name: 'Prometheus',
    desc: '메트릭 수집 및 쿼리',
    url: 'http://localhost:19090',
    color: '#E6522C',
    icon: '🔥',
  },
  {
    name: 'shop-user API',
    desc: '/actuator/prometheus',
    url: 'http://localhost:18083/actuator/prometheus',
    color: '#4A90D9',
    icon: '👤',
  },
  {
    name: 'shop-product API',
    desc: '/actuator/prometheus',
    url: 'http://localhost:18081/actuator/prometheus',
    color: '#4A90D9',
    icon: '📦',
  },
  {
    name: 'shop-order API',
    desc: '/actuator/prometheus',
    url: 'http://localhost:18082/actuator/prometheus',
    color: '#4A90D9',
    icon: '🛒',
  },
  {
    name: 'shop-review API',
    desc: '/actuator/prometheus',
    url: 'http://localhost:18084/actuator/prometheus',
    color: '#4A90D9',
    icon: '⭐',
  },
]

export default function Admin() {
  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>관리자 페이지</h2>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16 }}>
        {TOOLS.map((tool) => (
          <button
            key={tool.name}
            onClick={() => window.open(tool.url, '_blank')}
            style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'flex-start',
              gap: 8,
              padding: '20px 24px',
              border: `2px solid ${tool.color}`,
              borderRadius: 12,
              background: '#fff',
              cursor: 'pointer',
              textAlign: 'left',
              transition: 'background 0.15s',
            }}
            onMouseEnter={e => e.currentTarget.style.background = '#f8f8f8'}
            onMouseLeave={e => e.currentTarget.style.background = '#fff'}
          >
            <span style={{ fontSize: 28 }}>{tool.icon}</span>
            <span style={{ fontWeight: 'bold', fontSize: 16, color: tool.color }}>{tool.name}</span>
            <span style={{ fontSize: 13, color: '#666' }}>{tool.desc}</span>
            <span style={{ fontSize: 12, color: '#aaa', wordBreak: 'break-all' }}>{tool.url}</span>
          </button>
        ))}
      </div>
    </div>
  )
}
