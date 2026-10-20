-- 作者：沙里飞 | 邮箱：1499058156@qq.com | 公司：青海老铁软件有限公司
-- 圆梦社区 MySQL 5.7+ 完整初始化脚本：创建数据库与全部 25 张表。
-- 表结构与 backend/src/main/resources/schema.sql 一致（后端启动时也会自动补建缺失的表）；修改表结构时两处同步更新。
-- 可重复执行：仅创建不存在的库和表，不删除、不覆盖已有数据。
-- 不包含任何账号数据：管理员由后端首次启动时根据 ADMIN_USERNAME / ADMIN_PASSWORD 环境变量创建。
-- 用法：mysql -u root -p < init-mysql.sql

CREATE DATABASE IF NOT EXISTS dream_realize DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dream_realize;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS members (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(40) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  nickname VARCHAR(40) NOT NULL,
  bio VARCHAR(500) NOT NULL DEFAULT '',
  skills VARCHAR(200) NOT NULL DEFAULT '',
  city VARCHAR(60) NOT NULL DEFAULT '',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 旧注册流程的历史凭证表保留，不再签发或用于注册/评论校验。
CREATE TABLE IF NOT EXISTS admission_passes (
  token_hash VARCHAR(64) PRIMARY KEY,
  expires_at TIMESTAMP NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS registration_consents (
  member_id BIGINT PRIMARY KEY,
  notice_version VARCHAR(40) NOT NULL,
  notice_sha256 VARCHAR(64) NOT NULL,
  notice_snapshot TEXT NOT NULL,
  accepted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS comment_qualifications (
  member_id BIGINT PRIMARY KEY,
  exam_version VARCHAR(40) NOT NULL,
  score INT NOT NULL,
  passed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS sessions (
  token_hash VARCHAR(64) PRIMARY KEY,
  member_id BIGINT NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS dreams (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  story VARCHAR(5000) NOT NULL DEFAULT '',
  category VARCHAR(30) NOT NULL,
  help_type VARCHAR(30) NOT NULL,
  reward_type VARCHAR(20) NOT NULL DEFAULT 'FREE',
  contact_mode VARCHAR(20) NOT NULL DEFAULT 'BOTH',
  city VARCHAR(60) NOT NULL DEFAULT '',
  anonymous BOOLEAN NOT NULL DEFAULT FALSE,
  target_amount DECIMAL(14,2),
  received_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS dream_updates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  dream_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (dream_id) REFERENCES dreams(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS help_requests (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  dream_id BIGINT NOT NULL,
  helper_id BIGINT NOT NULL,
  offer VARCHAR(1000) NOT NULL,
  reward_type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (dream_id, helper_id),
  FOREIGN KEY (dream_id) REFERENCES dreams(id),
  FOREIGN KEY (helper_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  help_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (help_id) REFERENCES help_requests(id),
  FOREIGN KEY (sender_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS media_files (
  id VARCHAR(36) PRIMARY KEY,
  member_id BIGINT NOT NULL,
  file_name VARCHAR(48) NOT NULL UNIQUE,
  kind VARCHAR(10) NOT NULL,
  content_type VARCHAR(40) NOT NULL,
  size_bytes BIGINT NOT NULL,
  dream_id BIGINT,
  update_id BIGINT,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (member_id) REFERENCES members(id),
  FOREIGN KEY (dream_id) REFERENCES dreams(id),
  FOREIGN KEY (update_id) REFERENCES dream_updates(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS member_avatars (
  member_id BIGINT PRIMARY KEY,
  media_id VARCHAR(36) NOT NULL UNIQUE,
  FOREIGN KEY (member_id) REFERENCES members(id),
  FOREIGN KEY (media_id) REFERENCES media_files(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS administrators (
  member_id BIGINT PRIMARY KEY,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS dream_reactions (
  dream_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  reaction VARCHAR(12) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (dream_id, member_id),
  FOREIGN KEY (dream_id) REFERENCES dreams(id),
  FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS dream_comments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  dream_id BIGINT NOT NULL,
  member_id BIGINT NOT NULL,
  -- H2 按 UTF-16 计算列长度；接口仍限制为 1000 个 Unicode 字符。
  content VARCHAR(2000) NOT NULL,
  anonymous BOOLEAN NOT NULL DEFAULT FALSE,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_comment_dream (dream_id, id),
  FOREIGN KEY (dream_id) REFERENCES dreams(id),
  FOREIGN KEY (member_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS comment_moderation (
  comment_id BIGINT PRIMARY KEY,
  hidden BOOLEAN NOT NULL DEFAULT FALSE,
  reason VARCHAR(500) NOT NULL,
  admin_id BIGINT NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (comment_id) REFERENCES dream_comments(id),
  FOREIGN KEY (admin_id) REFERENCES administrators(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 原图不复制；来源评论被删除或下架时，收藏与转发同步不可用。
CREATE TABLE IF NOT EXISTS comment_attachments (
  comment_id BIGINT NOT NULL,
  media_id VARCHAR(36) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  is_source BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (comment_id, media_id),
  INDEX idx_comment_media (media_id, is_source),
  FOREIGN KEY (comment_id) REFERENCES dream_comments(id),
  FOREIGN KEY (media_id) REFERENCES media_files(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS media_favorites (
  member_id BIGINT NOT NULL,
  media_id VARCHAR(36) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (member_id, media_id),
  FOREIGN KEY (member_id) REFERENCES members(id),
  FOREIGN KEY (media_id) REFERENCES media_files(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS admin_sessions (
  token_hash VARCHAR(64) PRIMARY KEY,
  member_id BIGINT NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  FOREIGN KEY (member_id) REFERENCES administrators(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS member_restrictions (
  member_id BIGINT PRIMARY KEY,
  mode VARCHAR(20) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  expires_at TIMESTAMP NULL,
  admin_id BIGINT NOT NULL,
  revision INT NOT NULL DEFAULT 1,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (member_id) REFERENCES members(id),
  FOREIGN KEY (admin_id) REFERENCES administrators(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS dream_moderation (
  dream_id BIGINT PRIMARY KEY,
  hidden BOOLEAN NOT NULL DEFAULT FALSE,
  reason VARCHAR(500) NOT NULL,
  admin_id BIGINT NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (dream_id) REFERENCES dreams(id),
  FOREIGN KEY (admin_id) REFERENCES administrators(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS update_moderation (
  update_id BIGINT PRIMARY KEY,
  hidden BOOLEAN NOT NULL DEFAULT FALSE,
  reason VARCHAR(500) NOT NULL,
  admin_id BIGINT NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (update_id) REFERENCES dream_updates(id),
  FOREIGN KEY (admin_id) REFERENCES administrators(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS moderation_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  admin_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  action VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  expires_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (admin_id) REFERENCES administrators(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 成员之间的私信：member_low < member_high，一对成员只有一个会话；*_read_id 记录双方各自已读到的消息编号。
CREATE TABLE IF NOT EXISTS direct_conversations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_low BIGINT NOT NULL,
  member_high BIGINT NOT NULL,
  low_read_id BIGINT NOT NULL DEFAULT 0,
  high_read_id BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (member_low, member_high),
  FOREIGN KEY (member_low) REFERENCES members(id),
  FOREIGN KEY (member_high) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- (conversation_id, id) 唯一约束同时作为按会话分页读取的索引，兼容 MySQL 5.7 与 H2。
CREATE TABLE IF NOT EXISTS direct_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  conversation_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (conversation_id, id),
  FOREIGN KEY (conversation_id) REFERENCES direct_conversations(id),
  FOREIGN KEY (sender_id) REFERENCES members(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 用户意见反馈：status 为 NEW / PROCESSING / RESOLVED；reply 是后台回复，会展示给提交者。
CREATE TABLE IF NOT EXISTS member_feedback (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id BIGINT NOT NULL,
  category VARCHAR(20) NOT NULL,
  content VARCHAR(1000) NOT NULL,
  contact VARCHAR(100) NOT NULL DEFAULT '',
  status VARCHAR(20) NOT NULL DEFAULT 'NEW',
  reply VARCHAR(500) NOT NULL DEFAULT '',
  admin_id BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (member_id) REFERENCES members(id),
  FOREIGN KEY (admin_id) REFERENCES administrators(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
