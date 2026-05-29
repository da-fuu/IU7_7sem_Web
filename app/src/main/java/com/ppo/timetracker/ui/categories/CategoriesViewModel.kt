package com.ppo.timetracker.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.IUserRepository
import com.ppo.domain.usecase.*
import com.ppo.timetracker.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class CategoriesViewModel(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val manageUserCategoryUseCase: ManageUserCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    private val adminManageBaseCategoryUseCase: AdminManageBaseCategoryUseCase,
    private val adminDeleteBaseCategoryUseCase: AdminDeleteBaseCategoryUseCase,
    private val userRepository: IUserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    fun refresh() {
        val userId = sessionManager.currentUserId ?: return
        viewModelScope.launch {
            _categories.value = getCategoriesUseCase(userId)
            _isAdmin.value = userRepository.getUserRole(userId) == UserRole.ADMIN
        }
    }

    fun addCategory(name: String, colorHex: String, isDefault: Boolean) {
        val userId = sessionManager.currentUserId ?: return
        viewModelScope.launch {
            val category = Category(
                id = CategoryId(UUID.randomUUID()),
                userId = if (isDefault) null else userId,
                name = name,
                colorHex = colorHex,
                isDefault = isDefault
            )
            if (isDefault) {
                adminManageBaseCategoryUseCase(category, userId)
            } else {
                manageUserCategoryUseCase(category)
            }
            refresh()
        }
    }

    fun deleteCategory(category: Category) {
        val userId = sessionManager.currentUserId ?: return
        viewModelScope.launch {
            if (category.isDefault) {
                adminDeleteBaseCategoryUseCase(category.id, userId)
            } else {
                deleteCategoryUseCase(category.id)
            }
            refresh()
        }
    }
}
