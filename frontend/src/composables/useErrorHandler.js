// composables/useErrorHandler.js
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const globalError = ref(null)
const isNavigating = ref(false)

export function useErrorHandler() {
  const router = useRouter()

  const handleError = (error, context = 'Unknown') => {
    console.error(`[${context}] Error:`, error)
    globalError.value = { error, context, timestamp: Date.now() }
    
    // Clear error after 5 seconds
    setTimeout(() => {
      globalError.value = null
    }, 5000)
  }

  const safeNavigate = async (to) => {
    if (isNavigating.value) return false
    
    try {
      isNavigating.value = true
      await router.push(to)
      return true
    } catch (error) {
      handleError(error, 'Navigation')
      // Force navigation as fallback
      window.location.href = router.resolve(to).href
      return false
    } finally {
      isNavigating.value = false
    }
  }

  const clearError = () => {
    globalError.value = null
  }

  return {
    globalError,
    isNavigating,
    handleError,
    safeNavigate,
    clearError
  }
}