class BluePrinterManager{

  // 工厂模式
  BluePrinterManager._internal();
  factory BluePrinterManager() =>_getInstance();
  static BluePrinterManager get instance => _getInstance();
  static BluePrinterManager _instance = BluePrinterManager._internal();
  var  isConnected = false;
  var connect_address = "";//蓝牙地址
  var connect_name = "";//蓝牙名称



  static BluePrinterManager _getInstance() {
    if (_instance == null) {
      _instance = new BluePrinterManager._internal();
    }
    return _instance;
  }




}