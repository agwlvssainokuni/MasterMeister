# Permission Integration Tests Documentation

## 概要

パーミッションシステムの統合テストケースを作成しました。これらのテストは以下の主要機能をカバーしています：

## 作成されたテストファイル

### 1. PermissionImportIntegrationTest.java
**目的**: YAML インポート機能の統合テスト

**テストケース**:
- **testImportYamlWithBasicPermissions**: 基本的な権限のYAMLインポート
- **testImportYamlWithHierarchicalPermissions**: 階層的権限（CONNECTION、SCHEMA、TABLE）のインポート
- **testImportYamlWithExpirationDates**: 有効期限付き権限のインポート
- **testImportYamlWithInvalidUserEmail**: 存在しないユーザーでのインポートエラー処理
- **testImportYamlWithInvalidConnectionId**: 存在しないコネクションでのインポートエラー処理
- **testImportYamlWithMalformedYaml**: 不正なYAML形式でのエラー処理
- **testImportYamlReplacesExistingPermissions**: 既存権限の置き換え動作

**検証内容**:
- YAMLからDBへの正確なデータインポート
- 階層的権限構造（CONNECTION > SCHEMA > TABLE > COLUMN）の適用
- エラーハンドリングとバリデーション
- トランザクション整合性

### 2. PermissionExportIntegrationTest.java
**目的**: YAML エクスポート機能の統合テスト

**テストケース**:
- **testExportBasicPermissions**: 基本的な権限のYAMLエクスポート
- **testExportWithExpirationDates**: 有効期限付き権限のエクスポート
- **testExportWithDeniedPermissions**: 拒否権限のエクスポート
- **testExportWithTemplates**: 権限テンプレートのエクスポート
- **testExportWithNoPermissions**: 権限が存在しない場合のエクスポート
- **testExportWithInvalidConnectionId**: 存在しないコネクションでのエラー処理
- **testRoundTripImportExport**: インポート→エクスポートの往復テスト

**検証内容**:
- DBからYAMLへの正確なデータエクスポート
- YAML構造の正確性（export_info、connection_info、users、templates）
- データ型変換の正確性
- 往復変換での整合性保証

### 3. PermissionValidationIntegrationTest.java
**目的**: 権限判定ロジックの統合テスト

**テストケース**:
- **testHierarchicalPermissionResolution**: 階層的権限解決の検証
- **testColumnLevelPermissionSpecificity**: カラムレベル権限の特異性
- **testAdminUserFullAccess**: 管理者ユーザーの全権限アクセス
- **testExpiredPermissions**: 期限切れ権限の無効化
- **testFuturePermissions**: 未来日付権限の無効化
- **testAnnotationBasedPermissionValidation**: @RequirePermission アノテーションでの検証
- **testAnnotationBasedAdminAccess**: アノテーションでの管理者アクセス
- **testSqlPermissionFiltering**: SQLクエリでの権限フィルタリング
- **testSqlWritePermissions**: SQL書き込み権限の検証
- **testSqlDeletePermissions**: SQL削除権限の検証
- **testComplexPermissionScenario**: 複雑な権限シナリオの統合テスト

**検証内容**:
- 4層階層権限モデル（CONNECTION → SCHEMA → TABLE → COLUMN）
- 上位権限による下位権限の継承
- 明示的な拒否権限の優先
- Spring AOP による権限チェック
- SQL解析による動的権限検証
- 時間ベースの権限制御

## 権限システムアーキテクチャ

### 階層的権限モデル
```
CONNECTION (最上位)
├── SCHEMA
    ├── TABLE
        └── COLUMN (最下位)
```

### 権限解決アルゴリズム
1. **最下位レベルチェック**: 該当するCOLUMNレベル権限を検索
2. **上位レベル継承**: TABLE → SCHEMA → CONNECTION の順で権限を検索
3. **明示的拒否**: どのレベルでも `granted: false` なら拒否
4. **管理者例外**: ADMINロールは全権限を自動付与

### テスト対象の主要コンポーネント

#### サービス層
- **PermissionAuthService**: 権限判定の中核ロジック
- **PermissionYamlService**: YAML インポート/エクスポート
- **PermissionManagementService**: 権限CRUD操作
- **SqlPermissionFilter**: SQL解析と権限検証

#### アノテーション
- **@RequirePermission**: メソッドレベル権限制御
- **PermissionAspect**: AOP による権限チェック実装

#### データモデル
- **UserPermissionEntity**: 権限データ（user_id, connection_id, scope, permission_type, granted, expires_at など）
- **PermissionExportData**: YAML構造のDTOモデル

## テスト設定

### テスト用データベース
- **H2 In-Memory**: テスト実行時に自動初期化
- **Profile**: `test` プロファイルで分離
- **Transaction**: 各テスト後に自動ロールバック

### テストデータ
各テストで以下を自動セットアップ:
- テストユーザー（通常ユーザー/管理者ユーザー）
- テストデータベースコネクション
- 各種権限設定

## 実行方法

```bash
# すべての統合テストを実行
./gradlew test --tests "*Integration*"

# 特定のテストクラスを実行
./gradlew test --tests "PermissionImportIntegrationTest"
./gradlew test --tests "PermissionExportIntegrationTest"
./gradlew test --tests "PermissionValidationIntegrationTest"
```

## カバレッジ

### 機能カバレッジ
- ✅ YAML インポート（正常系・異常系）
- ✅ YAML エクスポート（各種データパターン）
- ✅ 階層的権限判定
- ✅ 時間ベース権限制御
- ✅ SQL権限フィルタリング
- ✅ アノテーションベース権限制御
- ✅ 管理者特権処理

### エラーハンドリング
- ✅ 不正ユーザー・コネクション
- ✅ 不正YAML形式
- ✅ 期限切れ権限
- ✅ 権限拒否処理

### データ整合性
- ✅ トランザクション境界
- ✅ 既存データ置き換え
- ✅ 往復変換整合性

これらのテストケースにより、Master Data Management システムの権限管理機能が期待通りに動作することを包括的に検証できます。