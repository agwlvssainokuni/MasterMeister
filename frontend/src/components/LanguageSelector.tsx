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

import React from 'react'
import {useTranslation} from 'react-i18next'
import '../styles/components/LanguageSelector.css'

interface LanguageSelectorProps {
  className?: string
}

export const LanguageSelector: React.FC<LanguageSelectorProps> = (
  {
    className = '',
  }
) => {
  const {i18n} = useTranslation()

  const handleLanguageChange = (event: React.ChangeEvent<HTMLSelectElement>) => {
    const newLanguage = event.target.value
    i18n.changeLanguage(newLanguage)
    localStorage.setItem('language', newLanguage)
  }

  return (
    <div className={`language-selector ${className}`}>
      <select
        value={i18n.language}
        onChange={handleLanguageChange}
        className="language-select"
        aria-label="Language selection"
      >
        <option value="ja">日本語</option>
        <option value="en">English</option>
      </select>
    </div>
  )
}
