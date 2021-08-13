package com.flutter.gp.flutter_plugin_gprint;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.gprinter.command.EscCommand;
import com.gprinter.command.LabelCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static com.flutter.gp.flutter_plugin_gprint.DeviceConnFactoryManager.ACTION_QUERY_PRINTER_STATE;
import static com.flutter.gp.flutter_plugin_gprint.DeviceConnFactoryManager.CONN_STATE_DISCONNECT;

/**
 * FlutterPluginGprintPlugin
 */
public class FlutterPluginGprintPlugin implements FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private BluetoothAdapter mBluetoothAdapter;
    Context context;
    private ThreadPool threadPool;
    public static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int CONN_STATE_DISCONN = 0x007;
    private int id = 0;
    private static final int CONN_MOST_DEVICES = 0x11;
    private static final int CONN_PRINTER = 0x12;
    public static final int MESSAGE_UPDATE_PARAMETER = 0x009;
    private EventChannel.EventSink eventSink = null;
    private Result result_statues;
    /**
     * 使用打印机指令错误
     */
    private static final int PRINTER_COMMAND_ERROR = 0x008;
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "flutter_plugin_gprint");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();

        EventChannel eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(),"com.gh.gpprinter/event");
        eventChannel.setStreamHandler(streamHandler);

    }
    //事件派发流
    private EventChannel.StreamHandler streamHandler = new EventChannel.StreamHandler() {
        private BroadcastReceiver chargingStateChangeReceiver;
        @Override
        public void onListen(Object o, EventChannel.EventSink sink) {

            System.out.println("-------------6----------------");
            System.out.println(sink);
            System.out.println("---------------6--------------");
            eventSink = sink;
            chargingStateChangeReceiver = createChargingStateChangeReceiver(sink);
            IntentFilter filter = new IntentFilter();
            filter.addAction("ACTION_QUERY_PRINTER_STATE");
            context.registerReceiver(
                    chargingStateChangeReceiver, filter);
        }

        @Override
        public void onCancel(Object o) {

            eventSink = null;

            context.unregisterReceiver(chargingStateChangeReceiver);
            chargingStateChangeReceiver = null;
        }

    };

    private BroadcastReceiver createChargingStateChangeReceiver(final EventChannel.EventSink events) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                switch (action){
                    case "ACTION_QUERY_PRINTER_STATE":
                        Map params = new HashMap();
                        params.put("msg",intent.getStringExtra("state"));
                        params.put("event",intent.getStringExtra("event"));
                        params.put("code",intent.getIntExtra("code",0));
                        params.put("close",intent.getIntExtra("close",0));
                        Log.d("ssstate",params.toString());
//                        events.success(params.toMap());
                        break;
                }


            }
        };
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("getPlatformVersion")) {
            result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else if (call.method.equals("init_sdk")) {
            initBluetooth();


        } else if (call.method.equals("search")) {
            System.out.println("12");

            System.out.println(mBluetoothAdapter);

            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothAdapter.startDiscovery();


        } else if (call.method.equals("connect")) {
            final String mac = (String) call.arguments;
            System.out.println("mac");
            System.out.println(mac);
            closeport();
//            BluetoothPort
            //macaddress
            new DeviceConnFactoryManager.Build().setContext(context).setId(0).setConnMethod(DeviceConnFactoryManager.CONN_METHOD.BLUETOOTH)
                    //设置连接的蓝牙mac地址
                    .setMacAddress(mac)
                    .build();
            threadPool = ThreadPool.getInstantiation();
            threadPool.addSerialTask(new Runnable() {
                @Override
                public void run() {
                    DeviceConnFactoryManager.getDeviceConnFactoryManagers()[0].openPort();
                }
            });



        } else if (call.method.equals("prints")) {
            final byte[] bytes = (byte[]) call.arguments;
            threadPool = ThreadPool.getInstantiation();
            threadPool.addSerialTask(new Runnable() {
                @Override
                public void run() {
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null ||
                            !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
//                        getConnState 未连接
                        return;
                    }
                    if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getCurrentPrinterCommand() == PrinterCommand.TSC) {
                        System.out.println("llll");
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

//                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(getReceipt(bitmap));
                        System.out.println(bitmap.getHeight());
                        System.out.println(bitmap.getWidth());

                        DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].sendDataImmediately(getLabel(bitmap));
                    } else {
//                      PRINTER_COMMAND_ERROR  打印机指令错误
                    }
                }
            });

        } else if (call.method.equals("disConnect")) {
            if (DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id] == null || !DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].getConnState()) {
                return;
            }
            DeviceConnFactoryManager deviceConnFactoryManager=DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id];
            if (deviceConnFactoryManager!= null&&deviceConnFactoryManager.getConnState()) {
                DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].closePort(id);
            }
        }else if (call.method.equals("stopScan")) {
            mBluetoothAdapter.cancelDiscovery();
        }else {
            result.notImplemented();
        }
    }
    private void initConnectReceive(){
        IntentFilter filter1 = new IntentFilter(ACTION_USB_PERMISSION);//USB访问权限广播
        filter1.addAction(ACTION_USB_DEVICE_DETACHED);//USB线拔出
        filter1.addAction(ACTION_QUERY_PRINTER_STATE);//查询打印机缓冲区状态广播，用于一票一控
        filter1.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE);//与打印机连接状态
        filter1.addAction(ACTION_USB_DEVICE_ATTACHED);//USB线插入
        context.registerReceiver(receiver, filter1);

    }

    private void closeport(){
        if(DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id]!=null&&DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort!=null) {
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].reader.cancel();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort.closePort();
            DeviceConnFactoryManager.getDeviceConnFactoryManagers()[id].mPort=null;
        }
    }

    private void initBluetooth() {
        // Get the local Bluetooth adapter
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        // Register for broadcasts when discovery has finished
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//蓝牙状态改变
//        filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE);//与打印机连接状态

        context.registerReceiver(mFindBlueToothReceiver, filter);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        System.out.println("128888");
        System.out.println(mBluetoothAdapter);


        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            //Bluetooth is not supported by the device
        } else {
            // If BT is not on, request that it be enabled.
            // setupChat() will then be called during onActivityResult
            if (!mBluetoothAdapter.isEnabled()) {
                  Log.d("bb","蓝牙未打开");
                channel.invokeMethod("BluStatue", "PoweredOff");

                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                context.startActivityForResult(enableIntent,1);
            } else {

            }
            initConnectReceive();

        }

    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println("  connection come");

          if (DeviceConnFactoryManager.ACTION_CONN_STATE.equals(action)) {
                int state = intent.getIntExtra(DeviceConnFactoryManager.STATE, -1);
                int deviceId = intent.getIntExtra(DeviceConnFactoryManager.DEVICE_ID, -1);
                switch (state) {
                    case CONN_STATE_DISCONNECT:
                        System.out.println("  connection is lost");
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                channel.invokeMethod("connectResult", "dis_connect");
                            }
                        });

//            connection is lost
                        break;
                    case DeviceConnFactoryManager.CONN_STATE_CONNECTING:
                        System.out.println(" connection is connecting");
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                channel.invokeMethod("connectResult", "connecting");
                            }
                        });
                        break;
                    case DeviceConnFactoryManager.CONN_STATE_CONNECTED:
                        DeviceConnFactoryManager deviceConnFactoryManager = DeviceConnFactoryManager.getDeviceConnFactoryManagers()[0];
                        final  String mac_adress =  deviceConnFactoryManager.getMacAddress();
                        System.out.println("连接成功");
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                channel.invokeMethod("connectResult", "success");
                            }
                        });

                        //
                        break;
                    case DeviceConnFactoryManager.CONN_STATE_FAILED:
                        System.out.println("failed");
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                channel.invokeMethod("connectResult", "fail");
                            }
                        });
                        break;
                    default:
                        break;
                }
            }


        }
    };

    private final BroadcastReceiver mFindBlueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                System.out.println(device);
                System.out.println("device88888");

                    final Map<String, String> map = new HashMap<>();
                    map.put("name", device.getName());
                    map.put("address", device.getAddress());
                    System.out.println(map);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            channel.invokeMethod("deviceFound", map);
                        }
                    });



            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
                    .equals(action)) {
                System.out.println("查询结束");
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                System.out.println("状态改变");
                int bluetooth_state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (bluetooth_state == BluetoothAdapter.STATE_OFF) {//关闭
                    System.out.println("蓝牙关闭");
                    channel.invokeMethod("BluStatue", "PoweredOff");

                }else if (bluetooth_state == BluetoothAdapter.STATE_ON) {//开启
                    System.out.println("蓝牙开启");
                    channel.invokeMethod("BluStatue", "open");

                }
            }
        }
    };

  

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }


    public static Vector<Byte> getLabel(Bitmap b) {
        LabelCommand tsc = new LabelCommand();
        // 设置标签尺寸宽高，按照实际尺寸设置 单位mm
        tsc.addSize(100, 85);
        // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0 单位mm
        tsc.addGap(2);
        // 设置打印方向
        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);
        // 开启带Response的打印，用于连续打印
        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
        // 设置原点坐标
        tsc.addReference(0, 0);
        //设置浓度
        // 撕纸模式开启
        tsc.addTear(EscCommand.ENABLE.ON);
        // 清除打印缓冲区
        tsc.addCls();

        // 绘制图片
        tsc.addBitmap(1, 0, LabelCommand.BITMAP_MODE.OVERWRITE, b.getWidth(), b);
        //绘制二维码

        // 打印标签
        tsc.addPrint(1, 1);
        // 打印标签后 蜂鸣器响
        tsc.addSound(2, 100);
        //开启钱箱
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand();
        // 发送数据

        return  datas;
    }
}
