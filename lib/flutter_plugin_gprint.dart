
import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';
typedef PrintPChanged<T> = void Function(T value);

class FlutterPluginGprint {
  static const MethodChannel _channel =
      const MethodChannel('flutter_plugin_gprint');
  PrintPChanged<Map<String, dynamic>>? _findBluetoothValueChanged;
  PrintPChanged<String>? _connectBluetoothValueChanged;
  PrintPChanged<Map>? _printProgressValueChanged;
  PrintPChanged<String>? _connectStatuesChanged;

  bool  dis_connect=true;
  FlutterPluginGprint(){
    _channel.setMethodCallHandler(_onCall);

  }
  Future<String?> get iosPermission async {
    final String? grant = await  _channel.invokeMethod('getBlueGrant');
    return grant;
  }

  Future< bool?> get connectStatue async {

    return dis_connect;
  }

  Future _onCall(MethodCall call) async {
    switch (call.method) {
      case "deviceFound":

        if (_findBluetoothValueChanged!=null) {
          _findBluetoothValueChanged!(
              Map<String, dynamic>.from(call.arguments as Map));
        }
        break;
      case "connectResult":
        if (_connectBluetoothValueChanged!=null) {
          _connectBluetoothValueChanged!(call.arguments);
          if (call.arguments != "success") {
            dis_connect = true;
            print('插件吊起 蓝牙断开了');
          }  else{
            dis_connect = false;

          }
        }


        break;

      case "BluStatue":
        if (_connectStatuesChanged!=null) {
          _connectStatuesChanged!(call.arguments);
        }





    }
    return null;
  }

  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
  /// 监听
  Future<void> addListener({PrintPChanged< Map<String, dynamic>>? findBluetoothValueChanged,
    PrintPChanged<String>? connectBluetoothValueChanged,
    PrintPChanged<Map>? printProgressValueChanged,PrintPChanged<String>? connectStatuesChanged,}) async {
    _findBluetoothValueChanged = findBluetoothValueChanged;
    _connectBluetoothValueChanged = connectBluetoothValueChanged;
    _printProgressValueChanged = printProgressValueChanged;
    _connectStatuesChanged = connectStatuesChanged;
  }
  //初始化蓝牙
  Future get  initSdk async{
    await _channel.invokeMethod('init_sdk');
  }
//  搜索

  Future search() async{
    await _channel.invokeMethod("search");

  }

//连接
  Future connect(String uuid) async{
    print("uuid"+uuid);
    _channel.invokeMethod('connect',uuid);
  }

//打印
  Future prints(Uint8List bytes) async{

    _channel.invokeMethod('prints',bytes);


  }
  //停止搜索
  Future stopScan() async{
    _channel.invokeMethod('stopScan');


  }
  //断开

  Future disConnect() async{
    _channel.invokeMethod('disConnect');


  }
}
