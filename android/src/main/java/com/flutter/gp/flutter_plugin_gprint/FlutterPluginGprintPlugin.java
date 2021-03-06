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
     * ???????????????????????????
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
    //???????????????
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
                    //?????????????????????mac??????
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
//                        getConnState ?????????
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
//                      PRINTER_COMMAND_ERROR  ?????????????????????
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
        IntentFilter filter1 = new IntentFilter(ACTION_USB_PERMISSION);//USB??????????????????
        filter1.addAction(ACTION_USB_DEVICE_DETACHED);//USB?????????
        filter1.addAction(ACTION_QUERY_PRINTER_STATE);//?????????????????????????????????????????????????????????
        filter1.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE);//????????????????????????
        filter1.addAction(ACTION_USB_DEVICE_ATTACHED);//USB?????????
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
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//??????????????????
//        filter.addAction(DeviceConnFactoryManager.ACTION_CONN_STATE);//????????????????????????

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
                  Log.d("bb","???????????????");
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
                        System.out.println("????????????");
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
                System.out.println("????????????");
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                System.out.println("????????????");
                int bluetooth_state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (bluetooth_state == BluetoothAdapter.STATE_OFF) {//??????
                    System.out.println("????????????");
                    channel.invokeMethod("BluStatue", "PoweredOff");

                }else if (bluetooth_state == BluetoothAdapter.STATE_ON) {//??????
                    System.out.println("????????????");
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
        // ??????????????????????????????????????????????????? ??????mm
        tsc.addSize(100, 85);
        // ?????????????????????????????????????????????????????????????????????????????????0 ??????mm
        tsc.addGap(2);
        // ??????????????????
        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);
        // ?????????Response??????????????????????????????
        tsc.addQueryPrinterStatus(LabelCommand.RESPONSE_MODE.ON);
        // ??????????????????
        tsc.addReference(0, 0);
        //????????????
        // ??????????????????
        tsc.addTear(EscCommand.ENABLE.ON);
        // ?????????????????????
        tsc.addCls();

        // ????????????
        tsc.addBitmap(1, 0, LabelCommand.BITMAP_MODE.OVERWRITE, b.getWidth(), b);
        //???????????????

        // ????????????
        tsc.addPrint(1, 1);
        // ??????????????? ????????????
        tsc.addSound(2, 100);
        //????????????
        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);
        Vector<Byte> datas = tsc.getCommand();
        // ????????????

        return  datas;
    }
}
