/*
 * Copyright 2025 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import apiClient from './apiClient'
import {API_ENDPOINTS} from '../config/config'
import type {ApiResponse, UserSummaryResult as ApiUserSummaryResult} from '../types/api'
import type {PendingUser} from '../types/frontend'

class AdminService {
  async getPendingUsers(): Promise<PendingUser[]> {
    const response = await apiClient.get<ApiResponse<ApiUserSummaryResult[]>>(
      API_ENDPOINTS.ADMIN.PENDING_USERS
    )

    if (!response.data.ok || !response.data.data) {
      throw new Error(response.data.error?.[0] || 'Failed to fetch pending users')
    }

    // Convert API DTOs to frontend types
    return response.data.data.map(user => ({
      id: user.id,
      username: user.username,
      email: user.email,
      registeredAt: new Date(user.createdAt),
      isPending: user.status === 'PENDING'
    }))
  }

  async approveUser(userId: number): Promise<void> {
    const response = await apiClient.post<ApiResponse<string>>(
      API_ENDPOINTS.ADMIN.APPROVE_USER(userId)
    )

    if (!response.data.ok) {
      throw new Error(response.data.error?.[0] || 'Failed to approve user')
    }
  }

  async rejectUser(userId: number): Promise<void> {
    const response = await apiClient.post<ApiResponse<string>>(
      API_ENDPOINTS.ADMIN.REJECT_USER(userId)
    )

    if (!response.data.ok) {
      throw new Error(response.data.error?.[0] || 'Failed to reject user')
    }
  }
}

export const adminService = new AdminService()
