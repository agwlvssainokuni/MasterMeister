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

import React, {useEffect, useState} from 'react'
import {useTranslation} from 'react-i18next'

interface ConfirmDialogProps {
  isOpen: boolean
  onClose: () => void
  onConfirm: () => void
  title: string
  message: string
  confirmText?: string
  cancelText?: string
  type?: 'default' | 'danger'
  loading?: boolean
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = (
  {isOpen, onClose, onConfirm, title, message, confirmText, cancelText, type = 'default', loading = false}
) => {
  const {t} = useTranslation()
  const [isVisible, setIsVisible] = useState(false)

  useEffect(() => {
    if (isOpen) {
      setIsVisible(true)
    }
  }, [isOpen])

  useEffect(() => {
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && isOpen && !loading) {
        onClose()
      }
    }

    if (isOpen) {
      document.addEventListener('keydown', handleEscape)
      document.body.style.overflow = 'hidden'
    }

    return () => {
      document.removeEventListener('keydown', handleEscape)
      document.body.style.overflow = 'auto'
    }
  }, [isOpen, loading, onClose])

  const handleOverlayClick = (event: React.MouseEvent) => {
    if (event.target === event.currentTarget && !loading) {
      onClose()
    }
  }

  const handleAnimationEnd = () => {
    if (!isOpen) {
      setIsVisible(false)
    }
  }

  const handleConfirm = () => {
    if (!loading) {
      onConfirm()
    }
  }

  const handleClose = () => {
    if (!loading) {
      onClose()
    }
  }

  if (!isVisible) return null

  const modalClasses = [
    'modal',
    'modal-confirm',
    type === 'danger' ? 'modal-danger' : ''
  ].filter(Boolean).join(' ')

  return (
    <div
      className={`modal-overlay ${isOpen ? 'open' : ''}`}
      onClick={handleOverlayClick}
      onTransitionEnd={handleAnimationEnd}
    >
      <div className={modalClasses}>
        <div className="modal-header">
          <h2 className="modal-title">{title}</h2>
          <button
            className="modal-close"
            onClick={handleClose}
            disabled={loading}
            aria-label={t('common.cancel')}
          >
            ×
          </button>
        </div>

        <div className="modal-body">
          <p>{message}</p>
        </div>

        <div className="modal-footer">
          <button
            className="button button-secondary"
            onClick={handleClose}
            disabled={loading}
          >
            {cancelText || t('common.cancel')}
          </button>
          <button
            className={`button ${type === 'danger' ? 'button-danger' : 'button-primary'}`}
            onClick={handleConfirm}
            disabled={loading}
          >
            {loading && <span className="spinner"></span>}
            {confirmText || t('common.confirm')}
          </button>
        </div>
      </div>
    </div>
  )
}
