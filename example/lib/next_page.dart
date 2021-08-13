

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_plugin_gprint_example/blue_printer_mananger.dart';

class NextPage extends StatelessWidget {
  const NextPage({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('bar'),
      ),
      body: Center(
        child: Container(
          child: GestureDetector(
            onTap: (){
              print(BluePrinterManager.instance.isConnected);
            },
            child:Container(
              constraints:  BoxConstraints(
                minWidth: 100,
                minHeight: 100,

              ),
              color: Colors.yellow,
            ),
          ),
        ),
      ),
    );
  }
}
