
importPackage(Packages['org.eclipse.paho.client.mqttv3']);
importClass('org.eclipse.paho.android.service.MqttAndroidClient');

const MQTT_URL = 'tcp://192.168.20.225:1883';
const CLIENT_ID = 'MOCK';
const TOPIC = 'ANDROID_MOCK';
const QOS = 2;
const USERNAME = 'device';
const PASSWORD = 'public';

const client = new MqttAndroidClient(context, MQTT_URL, CLIENT_ID);
const subscribeToTopic = () => {
  try {
    client.subscribe(
      TOPIC,
      QOS,
      null,
      new IMqttActionListener({
        onSuccess: token => {
          toast('MQTT 订阅成功');
        },
        onFailure: (token, error) => {
          toast('MQTT 订阅失败 ' + error);
        },
      })
    );
  } catch (error) {
    toast(error.message);
    alert('MQTT订阅错误\n\n"' + error.message);
  }
};

const initMQTT = () => {
  // 创建配置
  const mqttConnectOptions = new MqttConnectOptions();
  mqttConnectOptions.setAutomaticReconnect(true);
  mqttConnectOptions.setCleanSession(true);
  mqttConnectOptions.setUserName(USERNAME);
  mqttConnectOptions.setPassword(Array.from(PASSWORD));
  console.log('mqttConnectOptions', mqttConnectOptions);

  const callback = new MqttCallbackExtended({
    connectComplete: (reconnect, serverUri) => {
      if (reconnect) {
        subscribeToTopic();
        console.log('重新连接到MQTT');
      } else {
        console.log('连接到MQTT');
      }
    },
    connectionLost: () => {
      console.log('MQTT 连接丢失');
    },
    messageArrived: (topic, message) => {
      console.log('MQTT MESSAGE: ', topic, message);
    },
  })
  client.setCallback(callback);

  client.connect(
    mqttConnectOptions,
    null,
    new IMqttActionListener({
      onSuccess: () => {
        console.log('mqtt连接成功');
        subscribeToTopic();
      },
      onFailure: (token, error) => {
        console.error('mqtt连接失败', error);
        exit();
      },
    })
  );
};

const publish = (msg) => {
  // send message
  try {
    let javaString = new java.lang.String(msg);
    let byteArray = javaString.getBytes("UTF-8");
    client.publish(TOPIC, byteArray, QOS, false);
  } catch (error) {
    console.error('MQTT 发布失败', error);
  }
}

// 连接
initMQTT();
setTimeout(() => {
  toast('7秒后自动关闭');
  // send message
  publish('hello');
}, 3000);
// 断开并退出
setTimeout(() => {
  client.close();
  client.disconnect();
  toast('自动关闭并退出脚本');
  exit();
}, 10 * 1000);

// 阻塞 当前进程
setInterval(() => {
  //
}, 1000);
