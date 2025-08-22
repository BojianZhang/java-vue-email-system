import { defineStore } from 'pinia'
import { ref } from 'vue'
import { aliasApi } from '@/api/alias'

export const useAliasStore = defineStore('alias', () => {
  // 状态
  const aliases = ref([])
  const currentAlias = ref(null)
  const aliasStats = ref([])
  const loading = ref(false)

  // 获取用户别名列表
  const fetchAliases = async () => {
    loading.value = true
    try {
      const response = await aliasApi.getAliases()
      if (response.success) {
        aliases.value = response.data
        
        // 设置默认别名
        const defaultAlias = aliases.value.find(alias => alias.isDefault)
        if (defaultAlias) {
          currentAlias.value = defaultAlias
        } else if (aliases.value.length > 0) {
          currentAlias.value = aliases.value[0]
        }
      }
      return response
    } finally {
      loading.value = false
    }
  }

  // 获取别名统计信息
  const fetchAliasStats = async () => {
    try {
      const response = await aliasApi.getAliasStats()
      if (response.success) {
        aliasStats.value = response.data
      }
      return response.data
    } catch (error) {
      console.error('获取别名统计失败:', error)
      return []
    }
  }

  // 获取默认别名
  const fetchDefaultAlias = async () => {
    try {
      const response = await aliasApi.getDefaultAlias()
      if (response.success && response.data) {
        currentAlias.value = response.data
      }
      return response.data
    } catch (error) {
      console.error('获取默认别名失败:', error)
      return null
    }
  }

  // 创建新别名
  const createAlias = async (aliasData) => {
    try {
      const response = await aliasApi.createAlias(aliasData)
      if (response.success) {
        aliases.value.push(response.data)
        
        // 如果这是第一个别名，设为当前别名
        if (aliases.value.length === 1) {
          currentAlias.value = response.data
        }
      }
      return response
    } catch (error) {
      console.error('创建别名失败:', error)
      throw error
    }
  }

  // 更新别名
  const updateAlias = async (aliasId, aliasData) => {
    try {
      const response = await aliasApi.updateAlias(aliasId, aliasData)
      if (response.success) {
        // 更新本地状态
        const index = aliases.value.findIndex(alias => alias.id === aliasId)
        if (index > -1) {
          // 如果设为默认别名，需要取消其他别名的默认状态
          if (aliasData.isDefault) {
            aliases.value.forEach(alias => {
              alias.isDefault = alias.id === aliasId
            })
            currentAlias.value = aliases.value[index]
          }
          
          // 更新别名信息
          Object.assign(aliases.value[index], aliasData)
        }
      }
      return response
    } catch (error) {
      console.error('更新别名失败:', error)
      throw error
    }
  }

  // 删除别名
  const deleteAlias = async (aliasId) => {
    try {
      const response = await aliasApi.deleteAlias(aliasId)
      if (response.success) {
        // 从本地列表中移除
        const index = aliases.value.findIndex(alias => alias.id === aliasId)
        if (index > -1) {
          const deletedAlias = aliases.value[index]
          aliases.value.splice(index, 1)
          
          // 如果删除的是当前别名，切换到其他别名
          if (currentAlias.value && currentAlias.value.id === aliasId) {
            currentAlias.value = aliases.value.length > 0 ? aliases.value[0] : null
          }
        }
      }
      return response
    } catch (error) {
      console.error('删除别名失败:', error)
      throw error
    }
  }

  // 设置默认别名
  const setDefaultAlias = async (aliasId) => {
    try {
      const response = await aliasApi.setDefaultAlias(aliasId)
      if (response.success) {
        // 更新本地状态
        aliases.value.forEach(alias => {
          alias.isDefault = alias.id === aliasId
        })
        
        const newDefaultAlias = aliases.value.find(alias => alias.id === aliasId)
        if (newDefaultAlias) {
          currentAlias.value = newDefaultAlias
        }
      }
      return response
    } catch (error) {
      console.error('设置默认别名失败:', error)
      throw error
    }
  }

  // 切换到指定别名
  const switchToAlias = async (aliasId) => {
    try {
      const response = await aliasApi.switchToAlias(aliasId)
      if (response.success) {
        currentAlias.value = response.data
      }
      return response
    } catch (error) {
      console.error('切换别名失败:', error)
      throw error
    }
  }

  // 检查别名地址是否可用
  const checkAliasAvailability = async (aliasAddress, domainId) => {
    try {
      const response = await aliasApi.checkAliasAvailability(aliasAddress, domainId)
      return response.success ? response.available : false
    } catch (error) {
      console.error('检查别名可用性失败:', error)
      return false
    }
  }

  // 根据ID获取别名
  const getAliasById = (aliasId) => {
    return aliases.value.find(alias => alias.id === aliasId)
  }

  // 获取默认别名
  const getDefaultAlias = () => {
    return aliases.value.find(alias => alias.isDefault) || aliases.value[0]
  }

  // 添加别名到列表
  const addAlias = (alias) => {
    aliases.value.push(alias)
    
    // 如果这是第一个别名或者是默认别名，设为当前别名
    if (aliases.value.length === 1 || alias.isDefault) {
      currentAlias.value = alias
      
      // 如果设为默认，取消其他别名的默认状态
      if (alias.isDefault) {
        aliases.value.forEach(a => {
          if (a.id !== alias.id) {
            a.isDefault = false
          }
        })
      }
    }
  }

  // 移除别名
  const removeAlias = (aliasId) => {
    const index = aliases.value.findIndex(alias => alias.id === aliasId)
    if (index > -1) {
      const removedAlias = aliases.value[index]
      aliases.value.splice(index, 1)
      
      // 如果移除的是当前别名，切换到其他别名
      if (currentAlias.value && currentAlias.value.id === aliasId) {
        currentAlias.value = aliases.value.length > 0 ? aliases.value[0] : null
      }
    }
  }

  // 更新别名信息
  const updateAliasInfo = (aliasId, updates) => {
    const alias = aliases.value.find(a => a.id === aliasId)
    if (alias) {
      Object.assign(alias, updates)
      
      // 如果更新的是当前别名
      if (currentAlias.value && currentAlias.value.id === aliasId) {
        Object.assign(currentAlias.value, updates)
      }
    }
  }

  // 清空状态
  const clearAliases = () => {
    aliases.value = []
    currentAlias.value = null
    aliasStats.value = []
  }

  // 获取别名的统计信息
  const getAliasStats = (aliasId) => {
    return aliasStats.value.find(stat => stat.aliasId === aliasId)
  }

  // 更新别名统计信息
  const updateAliasStats = (aliasId, stats) => {
    const index = aliasStats.value.findIndex(stat => stat.aliasId === aliasId)
    if (index > -1) {
      Object.assign(aliasStats.value[index], stats)
    } else {
      aliasStats.value.push({ aliasId, ...stats })
    }
  }

  return {
    // 状态
    aliases,
    currentAlias,
    aliasStats,
    loading,
    
    // 方法
    fetchAliases,
    fetchAliasStats,
    fetchDefaultAlias,
    createAlias,
    updateAlias,
    deleteAlias,
    setDefaultAlias,
    switchToAlias,
    checkAliasAvailability,
    getAliasById,
    getDefaultAlias,
    addAlias,
    removeAlias,
    updateAliasInfo,
    clearAliases,
    getAliasStats,
    updateAliasStats
  }
})