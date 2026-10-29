<template>
  <router-link to="/" class="brand-logo" aria-label="X Learning 首页">
    <span class="brand-logo-mark" aria-hidden="true">X</span>
    <span>X Learning</span>
  </router-link>
</template>

<style scoped>
.brand-logo {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: var(--app-text);
  font-size: 20px;
  font-weight: 650;
  letter-spacing: -.5px;
  text-decoration: none;
  white-space: nowrap;
}
.brand-logo-mark {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: var(--app-accent);
  color: white;
  font-size: 20px;
  font-weight: 700;
}
.brand-logo:focus-visible {
  outline: 2px solid var(--app-accent);
  outline-offset: 5px;
  border-radius: 4px;
}
</style>
