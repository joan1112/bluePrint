#import "FlutterPluginGprintPlugin.h"
#import "ConnecterManager.h"
#import <CoreBluetooth/CoreBluetooth.h>
#import "EscCommand.h"
#import "TscCommand.h"
#define Manager [ConnecterManager sharedInstance]
#define WeakSelf(type) __weak typeof(type) weak##type = type
@interface FlutterPluginGprintPlugin()<CBCentralManagerDelegate>

@property (nonatomic, strong) FlutterMethodChannel* channel;
@property (nonatomic,strong)CBCentralManager *centralManager;
@property(nonatomic,strong)ConnecterManager *manager;
@property (nonatomic, strong) FlutterEventSink eventSink;
@property(nonatomic,copy)ConnectDeviceState state;
@property(nonatomic,copy)FlutterResult flutterResult;

@property(nonatomic,strong)NSMutableArray *devices;
@property(nonatomic,strong)NSMutableDictionary *dicts;
@end
@implementation FlutterPluginGprintPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  FlutterMethodChannel* channel = [FlutterMethodChannel
      methodChannelWithName:@"flutter_plugin_gprint"
            binaryMessenger:[registrar messenger]];
  FlutterPluginGprintPlugin* instance = [[FlutterPluginGprintPlugin alloc] init];
  [registrar addMethodCallDelegate:instance channel:channel];
    instance.channel = channel;
}


- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
  if ([@"getPlatformVersion" isEqualToString:call.method]) {
    result([@"iOS " stringByAppendingString:[[UIDevice currentDevice] systemVersion]]);
  }else if ([@"init_sdk" isEqualToString:call.method]) {//初始化

     self.manager = [ConnecterManager sharedInstance];
   
     
      self.dicts = [[NSMutableDictionary alloc] init];
      self.devices = [NSMutableArray array];


  } else if ([@"search" isEqualToString:call.method]) {//初始化

      if (Manager.bleConnecter == nil) {
          [Manager didUpdateState:^(NSInteger state) {
              switch (state) {
                  case CBCentralManagerStateUnsupported:
                      NSLog(@"The platform/hardware doesn't support Bluetooth Low Energy.");
                      break;
                  case CBCentralManagerStateUnauthorized:
                      NSLog(@"The app is not authorized to use Bluetooth Low Energy.");
                      break;
                  case CBCentralManagerStatePoweredOff:
                      NSLog(@"Bluetooth is currently powered off.");
                      break;
                  case CBCentralManagerStatePoweredOn:
                      [self startScane];
                      NSLog(@"Bluetooth power on");
                      break;
                  case CBCentralManagerStateUnknown:
                  default:
                      break;
              }
          }];
      } else {
          [self startScane];
      }

      result(nil);
      NSLog(@"====%@",@"end");




  }else if([@"connect" isEqualToString:call.method]){
      NSLog(@"OC====%@",call.arguments);
      WeakSelf(self);
      CBPeripheral *peripheral = self.dicts[call.arguments];
      [Manager connectPeripheral:peripheral options:nil timeout:2 connectBlack:^(ConnectState state) {
          if (CONNECT_STATE_CONNECTED == state) {
              [weakself.channel invokeMethod:@"connectResult" arguments:
               @"success"];
              [Manager stopScan];

          }else if(CONNECT_STATE_CONNECTING == state){//
              [weakself.channel invokeMethod:@"connectResult" arguments:
               @"connecting"];
          }else if(CONNECT_STATE_FAILT == state){//
              [weakself.channel invokeMethod:@"connectResult" arguments:
               @"fail"];
          }else if(NOT_FOUND_DEVICE == state){//
              [weakself.channel invokeMethod:@"connectResult" arguments:
               @"no_found"];
          }else{
              [weakself.channel invokeMethod:@"connectResult" arguments:
               @"dis_connect"];
          }
      }];

  }else if ([@"prints" isEqualToString:call.method]){
      NSLog(@"88989%@",call.arguments);
      FlutterStandardTypedData *dataImg = call.arguments;

      UIImage *img = [UIImage imageWithData:dataImg.data];
      NSLog(@"88989%f",img.size.width);

      TscCommand *command = [[TscCommand alloc]init];
      [command addSize:img.size.width :img.size.height];
      [command addGapWithM:1 withN:0];
      [command addReference:0 :0];
      [command addTear:@"ON"];
      [command addQueryPrinterStatus:ON];
      [command addCls];
      [command addBitmapwithX:0 withY:0 withMode:0 withWidth:img.size.width withImage:img];
      [command addPrint:1 :1];
      NSData * data = [command getCommand];
//      NSLog(@"%@",data);

                  WeakSelf(self);
                  [Manager write:data progress:^(NSUInteger total, NSUInteger progress) {
                      NSMutableDictionary *dic = [[NSMutableDictionary alloc] init];
                      dic[@"total"] = [NSString stringWithFormat:@"%lu",(unsigned long)total];
                      dic[@"progress"] = [NSString stringWithFormat:@"%lu",(unsigned long)progress];
                      [weakself.channel invokeMethod:@"printProgress" arguments:dic];
                  } receCallBack:^(NSData * resultData) {
                      NSString* result = [[NSString alloc] initWithData:resultData encoding:NSUTF8StringEncoding];
                      NSLog(@"%@",result);
                  }];

  }else if ([@"disConnect" isEqualToString:call.method]){
      [Manager close];

  }else if ([@"stopScan" isEqualToString:call.method]){
      [Manager stopScan];

  }else if ([@"getBlueGrant" isEqualToString:call.method]){
      self.centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil options:nil];
      self.flutterResult = result;
  } else {
    result(FlutterMethodNotImplemented);
  }
}

-(void)startScane{
    WeakSelf(self);

    [self.manager scanForPeripheralsWithServices:nil options:nil discover:^(CBPeripheral * _Nullable peripheral, NSDictionary<NSString *,id> * _Nullable advertisementData, NSNumber * _Nullable RSSI) {
                 NSLog(@"====%@",peripheral);
                 if (peripheral.name != nil) {
                     NSLog(@"name -> %@",peripheral.name);
                     NSMutableDictionary *dic = [NSMutableDictionary dictionary];
                     if (![[self.dicts allKeys]containsObject:peripheral.identifier.UUIDString]) {
                         [self.dicts setObject:peripheral forKey:peripheral.identifier.UUIDString];

                         [self.devices addObject:peripheral.identifier.UUIDString];
                     }
                     [dic setObject:peripheral.name forKey:@"name"];
                     [dic setObject:peripheral.identifier.UUIDString forKey:@"address"];
                     [weakself.channel invokeMethod:@"deviceFound" arguments:dic];


                 }


           }];



}

-(void)centralManagerDidUpdateState:(CBCentralManager *)central{

    if (@available(iOS 10.0, *)) {
        if(central.state == CBManagerStatePoweredOn)
        {
            NSLog(@"蓝牙设备开着");
            self.flutterResult(@"open");
        }
        else if (central.state == CBManagerStatePoweredOff)
        {
            NSLog(@"蓝牙设备关着");
            self.flutterResult(@"PoweredOff");
            
            
        }else {
            NSLog(@"该设备蓝牙未授权或者不支持蓝牙功能");
            self.flutterResult(@"蓝牙未授权或不支持");
        }
    } else {
        // Fallback on earlier versions
    }
}


@end
