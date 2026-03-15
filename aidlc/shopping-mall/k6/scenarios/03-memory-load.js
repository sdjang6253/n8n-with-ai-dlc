import http from 'k6/http'
import { sleep } from 'k6'

// 실행 예시:
//   k6 run -e SERVICE=http://localhost:18081 k6/scenarios/03-memory-load.js
// 예상 알람: HighMemoryUsage (JVM 힙 사용률 > 85%)

const TARGET_SERVICE = __ENV.SERVICE || 'http://localhost:18081'

export const options = {
  stages: [
    { duration: '30s', target: 5  },
    { duration: '2m',  target: 20 },
    { duration: '30s', target: 0  },
  ],
}

export default function () {
  http.get(`${TARGET_SERVICE}/simulate/memory`)
  sleep(0.5)
}
