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

package cherry.mastermeister.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * API統一レスポンス形式
 * <p>
 * 全てのAPIレスポンスはこの形式で返却される。
 * 成功時はok=true、data=レスポンス本体、error=null
 * 失敗時はok=false、data=null、error=エラーメッセージリスト
 * </p>
 *
 * @param <T> レスポンスデータの型
 * @param ok 処理成功フラグ
 * @param data レスポンス本体（成功時のみ）
 * @param error エラーメッセージリスト（失敗時のみ）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean ok,
        T data,
        List<String> error
) {
    /**
     * 成功レスポンスを作成する
     *
     * @param <T> レスポンスデータの型
     * @param data レスポンスデータ
     * @return 成功レスポンス
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * エラーレスポンスを作成する
     *
     * @param <T> レスポンスデータの型
     * @param errors エラーメッセージリスト
     * @return エラーレスポンス
     */
    public static <T> ApiResponse<T> error(List<String> errors) {
        return new ApiResponse<>(false, null, errors);
    }

    /**
     * エラーレスポンスを作成する（単一エラー）
     *
     * @param <T> レスポンスデータの型
     * @param error エラーメッセージ
     * @return エラーレスポンス
     */
    public static <T> ApiResponse<T> error(String error) {
        return error(List.of(error));
    }
}