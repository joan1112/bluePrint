import 'dart:typed_data';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'dart:async';
import 'dart:ui' as ui;
import 'package:flutter/services.dart';
import 'package:flutter_plugin_gprint/flutter_plugin_gprint.dart';
import 'package:flutter_plugin_gprint_example/blue_printer_mananger.dart';
import 'package:flutter_plugin_gprint_example/next_page.dart';
import 'package:permission_handler/permission_handler.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final flutterGPlugin = FlutterPluginGprint();

  List listDats = [];
  String current_statues = "none";
  GlobalKey _imageKey = GlobalKey();
  Uint8List _imgbtye = Uint8List(0);

  @override
  void initState() {
    super.initState();
    initPlatformState();
    BluePrinterManager manager =  BluePrinterManager.instance;
    manager.isConnected = true;
    initPlatformState();
  }
  initSdk(){
    if (Platform.isIOS) {
      var grant =  flutterGPlugin.iosPermission;
      if (grant == "open") {
        flutterGPlugin.initSdk;

      }

    }  else{

    }
    flutterGPlugin.initSdk;
    flutterGPlugin.addListener(findBluetoothValueChanged: (map) {
      final address = map['address'] ?? '';
      final name = map['name'] ?? '';
      if (listDats.contains('$name,$address') == false&& name.toString().length>0)
        listDats.add('$name,$address');
      if (mounted) {
        setState(() {});
      }
      if (mounted) {
        setState(() {});
      }
    }, connectBluetoothValueChanged: (String statues) {
      print('flutercc====' + statues);
      current_statues = statues;
      setState(() {});
    },connectStatuesChanged: (String str){
      print('fluterss====' + str);
    });
  }

  Future getBluthData() async {
    print('kkkkkk');
    await flutterGPlugin.search();
  }
  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    var status = await Permission.location.status;
    var bli = await Permission.bluetooth.status;
    print("00000" + status.toString() + bli.toString());
    if (Platform.isIOS) {
      initSdk();
    }  else{
      if (await Permission.location.request().isGranted) {
        if (await Permission.bluetooth.request().isGranted) {
          initSdk();
        }else{
          print('请打开蓝牙权限');
        }
       }
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.

  }


  List<Widget> _getWidget() {
    List<Widget> widgets = [];

    listDats.forEach((element) {
      Widget ww = Container(
        height: 50,
        child: GestureDetector(
          child: Text(element),
          onTap: () {
            String ads = element.toString().split(",").toList().last;
            print("9999===" + ads);
            flutterGPlugin.connect(ads);
          },
        ),
      );
      widgets.add(ww);
    });

    return widgets;
  }
  Widget _creatImageWidget() {
    print("111111");

  return RepaintBoundary(
        key: _imageKey,
        child: Container(
          width: 310,
          height: 217,
          color: Colors.white,
          child: Container(
            width: 100,
            decoration: new BoxDecoration(
//背景
              color: Colors.white,
              //设置四周圆角 角度
              // borderRadius: BorderRadius.all(Radius.circular(4.0)),
              //设置四周边框
              border: new Border.all(width: 1, color: Colors.black),
            ),
            child: Column(
              children: [
                Container(
                  height: 60,
                  child: Row(
                    children: [
                      Container(
                        height: 60,
                        child: Column(
                          children: [
                            Container(
                              height: 28,
                              padding: EdgeInsets.only(left: 10, right: 10),
                              child: Text(
                                "维修资格证",
                                style: TextStyle(fontWeight: FontWeight.bold),
                              ),
                              alignment: Alignment.center,
                            ),
                            Container(
                              height: 1.0,
                              width: 230,
                              color: Colors.black,
                            ),
                            Container(
                              height: 30,
                              padding: EdgeInsets.only(left: 0, right: 10),
                              child: Text('维修编号：9090909090',style: TextStyle(fontSize: 13),),
                              alignment: Alignment.centerLeft,
                            ),
                          ],
                        ),
                      ),
                      SizedBox(
                        width: 1,
                        height: 60,
                        child: DecoratedBox(
                          decoration: BoxDecoration(color: Colors.black),
                        ),
                      ),
                      Container(
                        height: 30,
                        child: Text('data'),
                        alignment: Alignment.center,
                      ),
                    ],
                  ),
                ),
                Divider(
                  height: 1.0,
                  indent: 0.0,
                  color: Colors.black,
                ),
                Container(
                  child: Text('水压试验压力：11kpa',style: TextStyle(fontSize: 13),),
                  padding: EdgeInsets.only(left: 10, right: 10),
                  alignment: Alignment.centerLeft,
                  height: 30,
                ),
                Divider(
                  height: 1.0,
                  indent: 0.0,
                  color: Colors.black,
                ),
                Container(
                  alignment: Alignment.center,
                  child: Row(
                    children: [
                      Container(
                        padding: EdgeInsets.only(left: 10, right: 20),
                        child: Text('总质量: 7kg',style: TextStyle(fontSize: 13),),
                      ),
                      SizedBox(
                        width: 1,
                        height: 30,
                        child: DecoratedBox(
                          decoration: BoxDecoration(color: Colors.black),
                        ),
                      ),
                      Container(
                        padding: EdgeInsets.only(left: 10, right: 20),
                        child: Text('检验员:  李超',style: TextStyle(fontSize: 13),),
                      ),
                    ],
                  ),
                  height: 30,
                ),
                Divider(
                  height: 1.0,
                  indent: 0.0,
                  color: Colors.black,
                ),
                Container(
                  alignment: Alignment.center,
                  child: Row(
                    children: [
                      Container(
                        padding: EdgeInsets.only(left: 10, right: 10),
                        child: Text('维修日期:  2020-11-23',style: TextStyle(fontSize: 13),),
                      ),
                      SizedBox(
                        width: 1,
                        height: 30,
                        child: DecoratedBox(
                          decoration: BoxDecoration(color: Colors.black),
                        ),
                      ),
                      Container(
                        padding: EdgeInsets.only(left: 10, right: 10),
                        child: Text('电话：8989898',style: TextStyle(fontSize: 13),),
                      ),
                    ],
                  ),
                  height: 30,
                ),
                Divider(
                  height: 1.0,
                  indent: 0.0,
                  color: Colors.black,
                ),
                Container(
                  padding: EdgeInsets.only(left: 10, right: 10),
                  alignment: Alignment.centerLeft,
                  child: Text('地址：安徽合肥',style: TextStyle(fontSize: 13),),
                  height: 30,
                ),
                Divider(
                  height: 1.0,
                  indent: 0.0,
                  color: Colors.black,
                ),
                Container(
                  padding: EdgeInsets.only(left: 10, right: 10),
                  alignment: Alignment.centerLeft,
                  child: Text('维修单位：北京华夏众安',style: TextStyle(fontSize: 13),),
                  height: 30,
                ),
              ],
            ),
          ),
        ));
  }
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              Container(
                height: 80,
                child: GestureDetector(
                  child: Text(' 扫描外设'),
                  onTap: () {
                    getBluthData();
                  },
                ),
              ),
              Column(
                children: _getWidget(),
              ),
              Text(
                current_statues,
                style: TextStyle(color: Colors.green),
              ),
              GestureDetector(
                child: Text('断开连接'),
                onTap: () {
                  flutterGPlugin.disConnect();
                },
              ),
              GestureDetector(
                child: Text('测试单例'),
                onTap: () {
                  print('0000'+ BluePrinterManager.instance.isConnected.toString());
                  Navigator.push(context, MaterialPageRoute(builder: (_) {
                    return new NextPage();
                  }));
                },
              ),
              SizedBox(
                height: 20,
              ),
              GestureDetector(
                child: Text('打印'),
                onTap: () async {
                  RenderRepaintBoundary boundary = _imageKey.currentContext
                      ?.findRenderObject() as RenderRepaintBoundary;
                  // pixelRatio: ui.window.devicePixelRatio,
                  ui.Image image = await boundary.toImage(
                      pixelRatio: ui.window.devicePixelRatio*2);
                  print(ui.window.devicePixelRatio);
                  print("ui.window.devicePixelRatio");

                  ByteData? byteData =
                  await image.toByteData(format: ui.ImageByteFormat.png);
                  Uint8List pngBytes = byteData!.buffer.asUint8List();
                  setState(() {
                    _imgbtye = pngBytes;
                  });

                  if (pngBytes != null) {
                    flutterGPlugin.prints(pngBytes);
                  }
                },
              ),
              _creatImageWidget(),

            ],
          ),
        ),
      ),
    );
  }
}
class NewHome extends StatelessWidget {
  const NewHome({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Container(
      child: Center(
        child: GestureDetector(
                child: Text('测试单例'),
                onTap: () {
                  print('0000'+ BluePrinterManager.instance.isConnected.toString());
                  Navigator.push(context, MaterialPageRoute(builder: (_) {
                    return new NextPage();
                  }));
                },
              ),
      ),
    );
  }
}

