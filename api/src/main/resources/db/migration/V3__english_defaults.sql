UPDATE users
SET display_name = 'System Administrator'
WHERE role = 'ADMIN' AND display_name = '系统管理员';
