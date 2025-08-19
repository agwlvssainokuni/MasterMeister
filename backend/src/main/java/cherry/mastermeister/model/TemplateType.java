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

package cherry.mastermeister.model;

public enum TemplateType {
    EMAIL_CONFIRMATION,      // (1) メールアドレス確認用（登録時）
    EMAIL_CONFIRMED,         // (2) メールアドレス確認済み（確認後）
    ACCOUNT_APPROVED,        // (3) アカウント承認通知
    ACCOUNT_REJECTED,        // (3) アカウント却下通知
    PASSWORD_RESET           // 将来拡張用
}
