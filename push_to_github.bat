@echo off
echo ========================================
echo 开始更新到GitHub...
echo ========================================
echo.

cd /d %~dp0

echo [1/7] 检查git是否安装...
git --version
if %errorlevel% neq 0 (
    echo 错误: 未找到git，请确保git已正确安装
    pause
    exit /b 1
)
echo.

echo [2/7] 配置git用户信息...
git config user.name "jurenpass"
git config user.email "35646088@qq.com"
echo 用户名: jurenpass
echo 邮箱: 35646088@qq.com
echo.

echo [3/7] 检查是否为git仓库...
if not exist .git (
    echo 初始化git仓库...
    git init
    git branch -M main
) else (
    echo git仓库已存在
)
echo.

echo [4/7] 检查远程仓库...
git remote -v | findstr "origin" >nul
if %errorlevel% neq 0 (
    echo 添加远程仓库...
    git remote add origin https://github.com/jurenpass/shiftWorkers.git
) else (
    echo 远程仓库已存在
    git remote -v
)
echo.

echo [5/7] 添加所有文件...
git add .
echo.

echo [6/7] 提交更改...
git commit -m "添加班组切换功能：下拉列表，对齐班组名称，宽度1.3倍，无分隔线"
echo.

echo [7/7] 推送到GitHub...
git push -u origin main

echo.
echo ========================================
echo 更新完成！
echo ========================================
pause
