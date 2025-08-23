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

package cherry.mastermeister.enums;

public enum TemplateType {
    REGISTER_EMAIL,         // (0) メール仮登録（email-firstフロー）
    REGISTER_USER,          // (1) アカウント本登録
    ACCOUNT_APPROVED,       // (3) アカウント承認通知
    ACCOUNT_REJECTED,       // (3) アカウント却下通知
    PASSWORD_RESET          // 将来拡張用
}
