// Distinct records are required: Vue Router treats language aliases as duplicate navigation.
export function homeRoutes(component) {
  return [
    { path: '/', alias: '/index.html', name: 'Landing', component },
    { path: '/zh/', name: 'LandingZh', component }
  ]
}
