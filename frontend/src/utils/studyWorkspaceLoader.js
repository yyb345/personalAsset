let pending

// Share the intent/idle preload with the router; a failed preload must remain retryable.
export function loadStudyWorkspace() {
  if (!pending) {
    pending = import('../views/dashboard/StudyWorkspace.vue').catch(error => {
      pending = undefined
      throw error
    })
  }
  return pending
}

export function preloadStudyWorkspace() {
  void loadStudyWorkspace().catch(() => {})
}
