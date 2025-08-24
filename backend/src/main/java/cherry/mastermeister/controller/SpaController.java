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

package cherry.mastermeister.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Single Page Application (SPA) のルーティングを処理するコントローラクラス。
 * React Routerのクライアントサイドルーティングをサポートするため、
 * 全てのSPA関連パスをindex.htmlにフォワードします。
 */
@Controller
public class SpaController {

    /**
     * SPA関連の全パスをindex.htmlにフォワードします。
     * React Routerがクライアントサイドでルーティングを処理できるようにします。
     *
     * @return index.htmlへのフォワード指示
     */
    @RequestMapping(value = {
            "/", "/login",
            "/register-email", "/register",
            "/dashboard",
            "/admin"
    })
    public String spa() {
        return "forward:/index.html";
    }
}
