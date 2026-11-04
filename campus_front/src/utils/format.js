export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

export function formatTime(value) {
  if (!value) return '-'
  return String(value).slice(0, 5)
}

export function formatMoney(value) {
  return Number(value || 0).toFixed(2)
}

export function shortText(value, length = 60) {
  if (!value) return ''
  return value.length > length ? `${value.slice(0, length)}…` : value
}

/**
 * 后端有些历史封面地址没有协议（例如 shturl.cc/xxx），
 * 浏览器会把它当成当前站点相对路径；这里统一补成 https。
 */
export function imageUrl(value) {
  if (!value) return ''
  const url = String(value).trim()
  if (!url) return ''
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('/') || url.startsWith('data:')) {
    return url
  }
  return `https://${url}`
}
