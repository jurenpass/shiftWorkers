const lunar = require('../../utils/lunar.js');

Page({
  data: {
    currentMonth: '2026年3月',
    currentTeam: '丁班',
    showTeamMenu: false,
    showDatePicker: false,
    selectedDate: '2026年03月19日',
    selectedWeekday: '星期四',
    selectedLunar: '二月初一',
    selectedShift: '下夜班',
    otherTeamsShift: '丙班（上夜班）甲班（休班）乙班（白班）',
    shifts: {},
    years: [],
    months: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
    days: [],
    selectedYear: 2026,
    selectedMonth: 3,
    selectedDay: 19
  },
  onLoad() {
    this.initCalendar();
  },
  initCalendar() {
    // 初始化默认选中日期
    const today = new Date();
    const year = today.getFullYear();
    const month = today.getMonth() + 1;
    const day = today.getDate();
    const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    const weekday = weekDays[today.getDay()];
    
    // 生成年份数组，从当前年份向前5年，向后10年
    const years = [];
    for (let i = year - 5; i <= year + 10; i++) {
      years.push(i);
    }
    
    // 生成月份数组
    const months = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12];
    
    // 生成当前月份的天数数组
    const daysInMonth = new Date(year, month, 0).getDate();
    const days = [];
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }
    
    const selectedDateStr = `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
    
    // 获取农历日期
    const lunar = this.getLunarDate(year, month, day);
    
    this.setData({
      years: years,
      months: months,
      days: days,
      currentMonth: `${year}年${month}月`,
      selectedDate: selectedDateStr, // 存储为 YYYY-MM-DD 格式，用于比较
      selectedDateDisplay: `${year}年${month.toString().padStart(2, '0')}月${day.toString().padStart(2, '0')}日`, // 用于显示的格式
      selectedWeekday: weekday,
      selectedLunar: lunar,
      selectedYear: year,
      selectedMonth: month,
      selectedDay: day
    });
    
    // 初始化班次数据和日历数据
    this.updateShifts();
    // 计算当前选中日期的班次
    this.updateSelectedShift();
    // 更新其他班组的班次信息
    this.updateOtherTeamsShift();
  },
  // 更新天数数组
  updateDays() {
    const year = this.data.selectedYear;
    const month = this.data.selectedMonth;
    const daysInMonth = new Date(year, month, 0).getDate();
    const days = [];
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }
    this.setData({ days: days });
  },
  // 处理日期选择变化
  onDateChange(e) {
    console.log('onDateChange called:', e);
    
    // 获取选择的日期
    const dateStr = e.detail.value;
    const dateParts = dateStr.split('-');
    const year = parseInt(dateParts[0]);
    const month = parseInt(dateParts[1]);
    const day = parseInt(dateParts[2]);
    
    // 更新当前月份显示
    this.setData({
      currentMonth: `${year}年${month}月`
    });
    
    // 更新选中日期
    const selectedDate = new Date(year, month - 1, day);
    const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    const weekday = weekDays[selectedDate.getDay()];
    
    // 计算农历日期
    const lunar = this.getLunarDate(year, month, day);
    
    // 计算当前班次
    const currentShift = this.calculateShift(this.data.currentTeam, `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`);
    
    const selectedDateStr = `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
    
    this.setData({
      selectedDate: selectedDateStr, // 存储为 YYYY-MM-DD 格式，用于比较
      selectedDateDisplay: `${year}年${month.toString().padStart(2, '0')}月${day.toString().padStart(2, '0')}日`, // 用于显示的格式
      selectedWeekday: weekday,
      selectedLunar: lunar,
      selectedShift: currentShift,
      selectedYear: year,
      selectedMonth: month,
      selectedDay: day
    });
    
    // 更新所有日期的班次，确保日历跳转到选定的月份
    this.updateShifts();
    
    // 更新其他班组的班次信息
    this.updateOtherTeamsShift();
  },
  // 选择年份
  selectYear(e) {
    const year = e.currentTarget.dataset.year;
    this.setData({ 
      selectedYear: year 
    });
    // 立即更新天数数组
    const month = this.data.selectedMonth;
    const daysInMonth = new Date(year, month, 0).getDate();
    const days = [];
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }
    this.setData({ 
      days: days,
      currentMonth: `${year}年${month}月`
    });
    // 更新日历数据
    this.updateShifts();
  },
  // 选择月份
  selectMonth(e) {
    const month = e.currentTarget.dataset.month;
    const year = this.data.selectedYear;
    this.setData({ 
      selectedMonth: month,
      currentMonth: `${year}年${month}月`
    });
    // 立即更新天数数组
    const daysInMonth = new Date(year, month, 0).getDate();
    const days = [];
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }
    this.setData({ days: days });
    // 更新日历数据
    this.updateShifts();
  },
  // 选择日期选择器中的日期
  selectDatePickerDay(e) {
    const day = e.currentTarget.dataset.day;
    this.setData({ selectedDay: day });
  },
  // 确认日期选择
  confirmDate() {
    const year = this.data.selectedYear;
    const month = this.data.selectedMonth;
    const day = this.data.selectedDay;
    
    console.log('confirmDate called with:', year, month, day);
    
    // 更新当前月份显示
    this.setData({
      currentMonth: `${year}年${month}月`,
      showDatePicker: false
    });
    
    // 更新选中日期
    const selectedDate = new Date(year, month - 1, day);
    const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    const weekday = weekDays[selectedDate.getDay()];
    
    // 计算农历日期
    const lunar = this.getLunarDate(year, month, day);
    
    // 计算当前班次
    const currentShift = this.calculateShift(this.data.currentTeam, `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`);
    
    const selectedDateStr = `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
    
    this.setData({
      selectedDate: selectedDateStr, // 存储为 YYYY-MM-DD 格式，用于比较
      selectedDateDisplay: `${year}年${month.toString().padStart(2, '0')}月${day.toString().padStart(2, '0')}日`, // 用于显示的格式
      selectedWeekday: weekday,
      selectedLunar: lunar,
      selectedShift: currentShift
    });
    
    // 更新所有日期的班次，确保日历跳转到选定的月份
    this.updateShifts();
    
    // 更新其他班组的班次信息
    this.updateOtherTeamsShift();
  },
  // 生成日历数据
  generateCalendarData() {
    const year = this.data.selectedYear;
    const month = this.data.selectedMonth;
    const calendarData = [];
    
    // 获取当前日期
    const today = new Date();
    const todayYear = today.getFullYear();
    const todayMonth = today.getMonth() + 1;
    const todayDay = today.getDate();
    const todayStr = `${todayYear}-${todayMonth.toString().padStart(2, '0')}-${todayDay.toString().padStart(2, '0')}`;
    
    // 获取当月第一天是星期几（0-6，0表示星期日）
    const firstDayOfMonth = new Date(year, month - 1, 1).getDay();
    
    // 生成上个月的最后几天
    const prevMonth = month === 1 ? 12 : month - 1;
    const prevYear = month === 1 ? year - 1 : year;
    const daysInPrevMonth = new Date(prevYear, prevMonth, 0).getDate();
    const prevDaysCount = firstDayOfMonth === 0 ? 6 : firstDayOfMonth - 1;
    
    for (let i = daysInPrevMonth - prevDaysCount + 1; i <= daysInPrevMonth; i++) {
      const date = `${prevYear}-${prevMonth.toString().padStart(2, '0')}-${i.toString().padStart(2, '0')}`;
      calendarData.push({
        date: date,
        day: i,
        month: prevMonth,
        year: prevYear,
        isCurrentMonth: false,
        isToday: date === todayStr,
        lunar: this.getLunarDate(prevYear, prevMonth, i),
        holiday: this.getHoliday(prevYear, prevMonth, i),
        shift: this.calculateShift(this.data.currentTeam, date)
      });
    }
    
    // 生成当前月份的所有日期
    const daysInMonth = new Date(year, month, 0).getDate();
    for (let i = 1; i <= daysInMonth; i++) {
      const date = `${year}-${month.toString().padStart(2, '0')}-${i.toString().padStart(2, '0')}`;
      calendarData.push({
        date: date,
        day: i,
        month: month,
        year: year,
        isCurrentMonth: true,
        isToday: date === todayStr,
        lunar: this.getLunarDate(year, month, i),
        holiday: this.getHoliday(year, month, i),
        shift: this.calculateShift(this.data.currentTeam, date)
      });
    }
    
    // 生成下个月的前几天，补齐6行7列
    const totalDays = 42; // 6行7列
    const nextDaysCount = totalDays - calendarData.length;
    const nextMonth = month === 12 ? 1 : month + 1;
    const nextYear = month === 12 ? year + 1 : year;
    
    for (let i = 1; i <= nextDaysCount; i++) {
      const date = `${nextYear}-${nextMonth.toString().padStart(2, '0')}-${i.toString().padStart(2, '0')}`;
      calendarData.push({
        date: date,
        day: i,
        month: nextMonth,
        year: nextYear,
        isCurrentMonth: false,
        isToday: date === todayStr,
        lunar: this.getLunarDate(nextYear, nextMonth, i),
        holiday: this.getHoliday(nextYear, nextMonth, i),
        shift: this.calculateShift(this.data.currentTeam, date)
      });
    }
    
    return calendarData;
  },
  
  // 获取农历日期（使用新的lunar.js库）
  getLunarDate(year, month, day) {
    // 特殊节日
    const specialHolidays = {
      '1-1': '元旦',
      '2-14': '情人节',
      '3-8': '妇女节',
      '3-12': '植树节',
      '4-1': '愚人节',
      '5-1': '劳动节',
      '5-4': '青年节',
      '5-12': '护士节',
      '5-14': '母亲节',
      '6-1': '儿童节',
      '6-21': '父亲节',
      '8-1': '建军节',
      '9-10': '教师节',
      '10-1': '国庆节',
      '12-25': '圣诞节'
    };
    
    // 节气
    const solarTerms = {
      '2-4': '立春',
      '2-19': '雨水',
      '3-5': '惊蛰',
      '3-20': '春分',
      '4-4': '清明',
      '4-20': '谷雨',
      '5-5': '立夏',
      '5-21': '小满',
      '6-5': '芒种',
      '6-21': '夏至',
      '7-7': '小暑',
      '7-23': '大暑',
      '8-7': '立秋',
      '8-23': '处暑',
      '9-7': '白露',
      '9-23': '秋分',
      '10-8': '寒露',
      '10-23': '霜降',
      '11-7': '立冬',
      '11-22': '小雪',
      '12-7': '大雪',
      '12-21': '冬至'
    };
    
    // 计算农历日期，用于判断节日
    let lunarResult = null;
    try {
      const date = new Date(year, month - 1, day);
      lunarResult = lunar.solarToLunar(date);
    } catch (error) {
      console.error('Lunar calculation error:', error);
    }
    
    // 检查农历节日
    if (lunarResult) {
      // 除夕：农历腊月最后一天
      if (lunarResult.month === '腊月' && (lunarResult.day === '廿九' || lunarResult.day === '三十')) {
        // 检查是否是腊月的最后一天
        const nextDay = new Date(year, month - 1, day + 1);
        let nextLunarResult = null;
        try {
          nextLunarResult = lunar.solarToLunar(nextDay);
        } catch (error) {
          console.error('Lunar calculation error for next day:', error);
        }
        if (nextLunarResult && nextLunarResult.month === '正月' && nextLunarResult.day === '初一') {
          return '除夕';
        }
      }
      // 春节：农历正月初一
      if (lunarResult.month === '正月' && lunarResult.day === '初一') {
        return '春节';
      }
      // 元宵节：农历正月十五
      if (lunarResult.month === '正月' && lunarResult.day === '十五') {
        return '元宵节';
      }
      // 端午节：农历五月初五
      if (lunarResult.month === '五月' && lunarResult.day === '初五') {
        return '端午节';
      }
      // 中秋节：农历八月十五
      if (lunarResult.month === '八月' && lunarResult.day === '十五') {
        return '中秋节';
      }
    }
    
    // 春分节气
    if (month === 3 && day === 20) {
      return '春分';
    }
    
    // 检查是否是特殊节日
    const key = `${month}-${day}`;
    if (specialHolidays[key]) {
      return specialHolidays[key];
    }
    
    // 特殊处理清明节
    if (month === 4) {
      // 计算清明节的正确日期
      const isQingming = lunar.isQingming(year, month, day);
      if (isQingming) {
        return '清明节';
      }
      // 如果不是清明节，且日期是4月4日或4月5日，不显示清明节气
      if (day === 4 || day === 5) {
        // 跳过清明节气，直接显示农历日期
      } else if (solarTerms[key]) {
        return solarTerms[key];
      }
    } else if (solarTerms[key]) {
      // 其他月份正常显示节气
      return solarTerms[key];
    }
    
    // 使用新的lunar.js库计算农历日期
    let lunarDate = '';
    try {
      const date = new Date(year, month - 1, day);
      const lunarResult = lunar.solarToLunar(date);
      lunarDate = lunarResult.simple;
    } catch (error) {
      console.error('Lunar calculation error:', error);
    }
    
    return lunarDate;
  },
  

  
  // 获取节日
  getHoliday(year, month, day) {
    // 这里可以添加更多节日信息
    const holidays = {
      '1-1': '元旦',
      '2-14': '情人节',
      '3-8': '妇女节',
      '3-12': '植树节',
      '4-1': '愚人节',
      '5-1': '劳动节',
      '5-4': '青年节',
      '5-12': '护士节',
      '5-14': '母亲节',
      '6-1': '儿童节',
      '6-21': '父亲节',
      '8-1': '建军节',
      '9-10': '教师节',
      '10-1': '国庆节',
      '12-25': '圣诞节'
    };
    
    const key = `${month}-${day}`;
    return holidays[key] || '';
  },
  
  // 更新所有日期的班次
  updateShifts() {
    const year = this.data.selectedYear;
    const month = this.data.selectedMonth;
    
    // 生成日历数据
    const calendarData = this.generateCalendarData();
    
    // 生成班次数据
    const shifts = {};
    calendarData.forEach(item => {
      shifts[item.date] = item.shift;
    });
    
    this.setData({
      shifts: shifts,
      calendarData: calendarData
    });
  },
  // 显示班组选择菜单
  showTeamMenu() {
    this.setData({ showTeamMenu: !this.data.showTeamMenu });
  },
  // 选择班组
  selectTeam(e) {
    const team = e.currentTarget.dataset.team;
    this.setData({
      currentTeam: team,
      showTeamMenu: false
    });
    // 更新所有日期的班次
    this.updateShifts();
    // 更新当前选中日期的班次
    this.updateSelectedShift();
    // 更新其他班组的班次信息
    this.updateOtherTeamsShift();
  },
  // 更新当前选中日期的班次
  updateSelectedShift() {
    // 解析当前选中日期
    const selectedDate = this.data.selectedDate;
    const dateParts = selectedDate.match(/(\d+)-(\d+)-(\d+)/);
    if (dateParts) {
      const year = dateParts[1];
      const month = dateParts[2];
      const day = dateParts[3];
      const dayStr = `${year}-${month}-${day}`;
      
      // 计算当前班组在当前日期的班次
      const currentShift = this.calculateShift(this.data.currentTeam, dayStr);
      this.setData({
        selectedShift: currentShift
      });
    }
  },
  // 根据班组和日期计算班次
  calculateShift(team, date) {
    // 班次顺序：白班 → 上夜班 → 下夜班 → 休班
    const shifts = ['白班', '上夜班', '下夜班', '休班'];
    const teams = ['甲班', '乙班', '丙班', '丁班'];
    
    // 解析日期
    const dateParts = date.split('-');
    const year = parseInt(dateParts[0]);
    const month = parseInt(dateParts[1]);
    const day = parseInt(dateParts[2]);
    
    // 计算从2026年3月1日到目标日期的天数差
    const baseYear = 2026;
    const baseMonth = 3;
    const baseDay = 1;
    
    // 创建基准日期和目标日期对象
    const baseDate = new Date(baseYear, baseMonth - 1, baseDay);
    const targetDate = new Date(year, month - 1, day);
    
    // 计算天数差（毫秒转天数）
    const totalDayDiff = Math.floor((targetDate - baseDate) / (1000 * 60 * 60 * 24));
    
    // 计算丁班的班次索引
    let dingBanShiftIndex;
    if (totalDayDiff >= 0) {
      // 未来日期：正序轮换
      dingBanShiftIndex = totalDayDiff % shifts.length;
    } else {
      // 过去日期：倒序轮换
      dingBanShiftIndex = (-totalDayDiff) % shifts.length;
      // 倒序轮换：白班 → 休班 → 下夜班 → 上夜班
      dingBanShiftIndex = (shifts.length - dingBanShiftIndex) % shifts.length;
    }
    
    // 计算班组索引
    const teamIndex = teams.indexOf(team);
    
    // 计算其他班组的班次：丁班班次 - (3 - teamIndex)
    // 丁班索引是3，其他班组的班次应该是丁班的前一个班次
    let shiftIndex;
    const teamDiff = 3 - teamIndex;
    if (totalDayDiff >= 0) {
      // 未来日期：正序计算
      shiftIndex = (dingBanShiftIndex - teamDiff) % shifts.length;
    } else {
      // 过去日期：倒序计算
      shiftIndex = (dingBanShiftIndex + teamDiff) % shifts.length;
    }
    
    return shifts[(shiftIndex + shifts.length) % shifts.length]; // 确保索引为正数
  },
  // 选择日期
  selectDay(e) {
    const day = e.currentTarget.dataset.day;
    
    // 解析日期
    const dateParts = day.split('-');
    const year = parseInt(dateParts[0]);
    const month = parseInt(dateParts[1]);
    const dayNum = parseInt(dateParts[2]);
    
    // 获取星期
    const selectedDate = new Date(year, month - 1, dayNum);
    const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
    const weekday = weekDays[selectedDate.getDay()];
    
    // 获取农历日期
    const lunar = this.getLunarDate(year, month, dayNum);
    
    // 计算当前班组在选中日期的班次
    const currentShift = this.calculateShift(this.data.currentTeam, day);
    
    this.setData({
      selectedDate: day, // 存储为 YYYY-MM-DD 格式，用于比较
      selectedDateDisplay: `${year}年${month}月${dayNum}日`, // 用于显示的格式
      selectedWeekday: weekday,
      selectedLunar: lunar,
      selectedShift: currentShift,
      selectedYear: year,
      selectedMonth: month,
      selectedDay: dayNum
    });
    
    // 更新其他班组的班次信息
    this.updateOtherTeamsShift();
  },
  // 更新其他班组的班次信息
  updateOtherTeamsShift() {
    const teams = ['甲班', '乙班', '丙班', '丁班'];
    const currentTeam = this.data.currentTeam;
    const selectedDate = this.data.selectedDate;
    
    let otherTeamsShift = '';
    
    teams.forEach((team) => {
      if (team !== currentTeam) {
        // 使用新的班次计算方法计算其他班组的班次
        const shift = this.calculateShift(team, selectedDate);
        otherTeamsShift += `${team}（${shift}） `;
      }
    });
    
    this.setData({ otherTeamsShift: otherTeamsShift.trim() });
  },
  // 查看班组统计
  onTeamStats() {
    wx.showToast({
      title: '班组统计功能开发中',
      icon: 'none'
    });
  },
  
  // 触摸开始事件
  touchStart(e) {
    this.setData({
      touchStartX: e.touches[0].clientX,
      touchStartY: e.touches[0].clientY,
      touchEndX: undefined,
      touchEndY: undefined
    });
  },
  
  // 触摸移动事件
  touchMove(e) {
    this.setData({
      touchEndX: e.touches[0].clientX,
      touchEndY: e.touches[0].clientY
    });
  },
  
  // 触摸结束事件
  touchEnd() {
    const { touchStartX, touchStartY, touchEndX, touchEndY } = this.data;
    
    // 检查是否有滑动动作
    if (touchEndX === undefined || touchEndY === undefined) {
      // 没有滑动，是点击事件，不执行切换操作
      return;
    }
    
    // 计算滑动距离
    const deltaX = touchEndX - touchStartX;
    const deltaY = touchEndY - touchStartY;
    
    // 检查是否是有效滑动
    if (Math.abs(deltaX) < 50 && Math.abs(deltaY) < 50) {
      // 滑动距离太小，视为点击事件，不执行切换操作
      return;
    }
    
    // 判断滑动方向
    if (Math.abs(deltaX) > Math.abs(deltaY)) {
      // 左右滑动 - 切换月份
      if (deltaX > 0) {
        // 向右滑动 - 上一个月
        this.changeMonth(-1);
      } else {
        // 向左滑动 - 下一个月
        this.changeMonth(1);
      }
    } else {
      // 上下滑动 - 切换年份
      if (deltaY > 0) {
        // 向下滑动 - 上一年
        this.changeYear(-1);
      } else {
        // 向上滑动 - 下一年
        this.changeYear(1);
      }
    }
    
    // 重置触摸坐标
    this.setData({
      touchStartX: undefined,
      touchStartY: undefined,
      touchEndX: undefined,
      touchEndY: undefined
    });
  },
  
  // 切换月份
  changeMonth(direction) {
    let { selectedYear, selectedMonth } = this.data;
    
    selectedMonth += direction;
    
    if (selectedMonth > 12) {
      selectedMonth = 1;
      selectedYear += 1;
    } else if (selectedMonth < 1) {
      selectedMonth = 12;
      selectedYear -= 1;
    }
    
    this.setData({
      selectedYear: selectedYear,
      selectedMonth: selectedMonth,
      currentMonth: `${selectedYear}年${selectedMonth}月`
    });
    
    // 更新天数数组
    const daysInMonth = new Date(selectedYear, selectedMonth, 0).getDate();
    const days = [];
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }
    this.setData({ days: days });
    
    // 更新日历数据
    this.updateShifts();
  },
  
  // 切换年份
  changeYear(direction) {
    let { selectedYear, selectedMonth } = this.data;
    
    selectedYear += direction;
    
    this.setData({
      selectedYear: selectedYear,
      currentMonth: `${selectedYear}年${selectedMonth}月`
    });
    
    // 更新天数数组
    const daysInMonth = new Date(selectedYear, selectedMonth, 0).getDate();
    const days = [];
    for (let i = 1; i <= daysInMonth; i++) {
      days.push(i);
    }
    this.setData({ days: days });
    
    // 更新日历数据
    this.updateShifts();
  }
})