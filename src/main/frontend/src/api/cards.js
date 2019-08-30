import client from '@/api/client'

async function getCards (username) {
  const response = await client.get(`/idcards/${username}`)
  return response.data
}

async function getPreview () {
  const response = await client.get('/idcards/preview')
  return response.data
}

async function requestNew ({ requestReason }) {
  await client.post('/idcards', requestReason, { headers: { 'X-Requested-With': 'fenixedu-id-cards-frontend', 'Content-Type': 'text/plain' } })
}

async function deliverCard (id) {
  await client.put(`/idcards/${id}/deliver`, null, { headers: { 'X-Requested-With': 'fenixedu-id-cards-frontend' } })
}

async function getAdminSession () {
  const response = await client.get('/idcards/deliver/admin-session')
  return response.data
}

async function submitUserMifare (request) {
  await client.put('/idcards/deliver/admin-session', request, { headers: { 'X-Requested-With': 'fenixedu-id-cards-frontend' } })
}

export default {
  getCards,
  getPreview,
  requestNew,
  deliverCard,
  getAdminSession,
  submitUserMifare
}
