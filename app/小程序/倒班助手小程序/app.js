App({
  onLaunch() {
    // 初始化数据
    this.initData();
  },
  initData() {
    // 模拟倒班数据
    const shiftData = {
      shifts: ['白班', '上夜班', '下夜班', '休班'],
      currentShift: '下夜班',
      currentProgress: 3/4,
      shiftInfo: [
        { name: '丁班', status: '下夜班' },
        { name: '丙班', status: '上夜班' },
        { name: '甲班', status: '休班' },
        { name: '乙班', status: '白班' }
      ]
    };
    wx.setStorageSync('shiftData', shiftData);
    
    // 模拟闹钟数据
    const alarmData = [
      { time: '03:55', name: '夜班接班', type: '倒班闹钟', enabled: false },
      { time: '06:33', name: '上白班', type: '倒班闹钟', enabled: true },
      { time: '07:36', name: '打卡', type: '普通闹钟', enabled: true },
      { time: '08:05', name: '打卡', type: '普通闹钟', enabled: true },
      { time: '18:38', name: '上夜班', type: '倒班闹钟', enabled: true },
      { time: '20:03', name: '下班打卡', type: '普通闹钟', enabled: true },
      { time: '23:55', name: '接班', type: '倒班闹钟', enabled: true }
    ];
    wx.setStorageSync('alarmData', alarmData);
  },
  globalData: {
    userInfo: null
  }
})