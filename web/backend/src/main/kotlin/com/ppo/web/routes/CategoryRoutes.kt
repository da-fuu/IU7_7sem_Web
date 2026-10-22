package com.ppo.web.routes

import com.ppo.domain.model.Category
import com.ppo.domain.model.CategoryId
import com.ppo.domain.model.UserRole
import com.ppo.domain.repository.ICategoryRepository
import com.ppo.web.dto.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import java.util.UUID

fun Route.categoryRoutes(categoryRepository: ICategoryRepository) {
    route("/api/v1/categories") {
        get {
            val authUser = call.getAuthUserOrRespond() ?: return@get
            val scope = call.request.queryParameters["scope"]?.lowercase() ?: "all"
            val all = categoryRepository.getAllCategories()
            val filtered = when (scope) {
                "default" -> all.filter { it.isDefault }
                "custom" -> all.filter { !it.isDefault && it.userId == authUser.userId }
                else -> all.filter { it.isDefault || it.userId == authUser.userId }
            }
            call.respond(HttpStatusCode.OK, CategoryListResponseDto(items = filtered.map { it.toDto() }))
        }

        post {
            val authUser = call.getAuthUserOrRespond() ?: return@post
            val req = try {
                call.receive<CategoryCreateRequestDto>()
            } catch (e: Exception) {
                return@post call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON")
            }

            if (req.name.isBlank()) {
                return@post call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Поле name обязательно")
            }
            if (!Regex("^#[0-9A-Fa-f]{6}$").matches(req.colorHex)) {
                return@post call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Цвет должен соответствовать формату ^#[0-9A-Fa-f]{6}$")
            }

            val isDefault = req.isDefault ?: false
            if (isDefault && authUser.role != UserRole.ADMIN) {
                return@post call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Только администратор может создавать базовые категории")
            }

            val ownerId = if (isDefault) null else authUser.userId
            val existing = categoryRepository.findCategoryByName(ownerId, req.name)
            if (existing != null) {
                return@post call.respondError(HttpStatusCode.Conflict, "CATEGORY_ALREADY_EXISTS", "Категория с названием '${req.name}' уже существует")
            }

            val category = Category(
                id = CategoryId(UUID.randomUUID()),
                userId = ownerId,
                name = req.name,
                colorHex = req.colorHex,
                isDefault = isDefault
            )

            categoryRepository.saveCategory(category)
            call.respond(HttpStatusCode.Created, category.toDto())
        }

        route("/{categoryId}") {
            get {
                val authUser = call.getAuthUserOrRespond() ?: return@get
                val catIdStr = call.parameters["categoryId"] ?: return@get call.respondError(
                    HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр categoryId обязателен"
                )
                val catId = try {
                    CategoryId(UUID.fromString(catIdStr))
                } catch (e: Exception) {
                    return@get call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID категории")
                }

                val category = categoryRepository.findCategoryById(catId)
                if (category == null) {
                    return@get call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Категория с ID '$catIdStr' не найдена")
                }

                if (!category.isDefault && category.userId != authUser.userId && authUser.role != UserRole.ADMIN) {
                    return@get call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой категории запрещен")
                }

                call.respond(HttpStatusCode.OK, category.toDto())
            }

            put {
                val authUser = call.getAuthUserOrRespond() ?: return@put
                val catIdStr = call.parameters["categoryId"] ?: return@put call.respondError(
                    HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр categoryId обязателен"
                )
                val catId = try {
                    CategoryId(UUID.fromString(catIdStr))
                } catch (e: Exception) {
                    return@put call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID категории")
                }

                val existing = categoryRepository.findCategoryById(catId)
                if (existing == null) {
                    return@put call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Категория '$catIdStr' не найдена")
                }

                if (existing.isDefault && authUser.role != UserRole.ADMIN) {
                    return@put call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Нельзя редактировать базовую категорию")
                }
                if (!existing.isDefault && existing.userId != authUser.userId && authUser.role != UserRole.ADMIN) {
                    return@put call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой категории запрещен")
                }

                val req = try {
                    call.receive<CategoryUpdateRequestDto>()
                } catch (e: Exception) {
                    return@put call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON")
                }

                if (req.name.isBlank()) {
                    return@put call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Поле name обязательно")
                }
                if (!Regex("^#[0-9A-Fa-f]{6}$").matches(req.colorHex)) {
                    return@put call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Некорректный HEX-цвет")
                }

                val updated = existing.copy(name = req.name, colorHex = req.colorHex)
                categoryRepository.saveCategory(updated)
                call.respond(HttpStatusCode.OK, updated.toDto())
            }

            patch {
                val authUser = call.getAuthUserOrRespond() ?: return@patch
                val catIdStr = call.parameters["categoryId"] ?: return@patch call.respondError(
                    HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр categoryId обязателен"
                )
                val catId = try {
                    CategoryId(UUID.fromString(catIdStr))
                } catch (e: Exception) {
                    return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID категории")
                }

                val existing = categoryRepository.findCategoryById(catId)
                if (existing == null) {
                    return@patch call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Категория '$catIdStr' не найдена")
                }

                if (existing.isDefault && authUser.role != UserRole.ADMIN) {
                    return@patch call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Нельзя редактировать базовую категорию")
                }
                if (!existing.isDefault && existing.userId != authUser.userId && authUser.role != UserRole.ADMIN) {
                    return@patch call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой категории запрещен")
                }

                val req = try {
                    call.receive<CategoryPatchRequestDto>()
                } catch (e: Exception) {
                    return@patch call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный JSON")
                }

                if (req.colorHex != null && !Regex("^#[0-9A-Fa-f]{6}$").matches(req.colorHex)) {
                    return@patch call.respondError(HttpStatusCode.BadRequest, "VALIDATION_ERROR", "Некорректный HEX-цвет")
                }

                val updated = existing.copy(
                    name = req.name ?: existing.name,
                    colorHex = req.colorHex ?: existing.colorHex
                )
                categoryRepository.saveCategory(updated)
                call.respond(HttpStatusCode.OK, updated.toDto())
            }

            delete {
                val authUser = call.getAuthUserOrRespond() ?: return@delete
                val catIdStr = call.parameters["categoryId"] ?: return@delete call.respondError(
                    HttpStatusCode.BadRequest, "BAD_REQUEST", "Параметр categoryId обязателен"
                )
                val catId = try {
                    CategoryId(UUID.fromString(catIdStr))
                } catch (e: Exception) {
                    return@delete call.respondError(HttpStatusCode.BadRequest, "BAD_REQUEST", "Некорректный UUID категории")
                }

                val existing = categoryRepository.findCategoryById(catId)
                if (existing == null) {
                    return@delete call.respondError(HttpStatusCode.NotFound, "CATEGORY_NOT_FOUND", "Категория '$catIdStr' не найдена")
                }

                if (existing.isDefault && authUser.role != UserRole.ADMIN) {
                    return@delete call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Нельзя удалить базовую категорию")
                }
                if (!existing.isDefault && existing.userId != authUser.userId && authUser.role != UserRole.ADMIN) {
                    return@delete call.respondError(HttpStatusCode.Forbidden, "FORBIDDEN", "Доступ к чужой категории запрещен")
                }

                categoryRepository.deleteCategory(catId)
                call.respond(HttpStatusCode.NoContent, Unit)
            }
        }
    }
}
