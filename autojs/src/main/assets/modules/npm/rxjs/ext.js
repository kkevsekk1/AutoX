let { Subscription, asyncScheduler } = require("./index.js");
const t = threads;
const mainTimer = timers.mainTimer;
const uiTimer = timers.uiTimer;

const ioScheduler = {
  bid: 0,
  schedule(work, delay, state) {
    const action = new SchedulerAction(this, work);
    return action.schedule(state, delay);
  },
  now() {
    return Date.now();
  },
  getexecutor() {
    return {
      id: this.bid++,
      th: null,
      run(fn, delay) {
        if (this.th) {
          this.th.setTimeout(fn, delay);
        } else {
          this.th = t.start(() => {
            setTimeout(fn, delay);
          });
        }
      },
      close() {
        this.th.interrupt();
        this.th = null;
        //console.log("关闭", this.id);
      },
      cancel() {
        //console.log("取消", this.id);
      },
    };
  },
};

const SchedulerAction = function (scheduler, work) {
  Subscription.call(this);
  this.work = work;
  //this.subAction = [];
  this.executor = scheduler.getexecutor();
};
SchedulerAction.prototype = Object.create(Subscription.prototype, {
  constructor: { value: SchedulerAction },
});
Object.assign(SchedulerAction.prototype, {
  schedule(state, delay) {
    if (this.closed) return new Subscription();
    const call = this.executor.run(() => {
      this.work.call(this, state);
    }, delay);
    const callAction = new Subscription();
    callAction.call = call;
    callAction.executor = this.executor;
    callAction.unsubscribe = function () {
      this.closed = true;
      const cancel = this.executor.cancel;
      if (cancel) {
        cancel.call(this.executor, this.call);
      }
    };
    return callAction;
  },
  unsubscribe() {
    this.closed = true;
    this.executor.close();
  },
});

const mainScheduler = {
  bid: 0,
  schedule(work, delay, state) {
    const action = new SchedulerAction(this, work);
    return action.schedule(state, delay);
  },
  now() {
    return Date.now();
  },
  getexecutor() {
    return {
      id: null,
      run(fn, delay) {
        const id = mainTimer.setTimeout(fn, delay);
        return id;
      },
      close() {
        //console.log("关闭");
      },
      cancel(id) {
        mainTimer.clearTimeout(id);
      },
    };
  },
};

const uiScheduler = {
  bid: 0,
  schedule(work, delay, state) {
    const action = new SchedulerAction(this, work);
    return action.schedule(state, delay);
  },
  now() {
    return Date.now();
  },
  getexecutor() {
    return {
      id: null,
      run(fn, delay) {
        const id = uiTimer.setTimeout(fn, delay);
        return id;
      },
      close() {
        //console.log("关闭");
      },
      cancel(id) {
        uiTimer.clearTimeout(id);
      },
    };
  },
};
const workScheduler = {
  bid: 0,
  schedule(work, delay, state) {
    const action = new SchedulerAction(this, work);
    return action.schedule(state, delay);
  },
  now() {
    return Date.now();
  },
  getexecutor() {
    return {
      id: null,
      run(fn, delay) {
        if (delay) {
          const id = uiTimer.setTimeout(() => {
            t.runTaskForThreadPool(fn);
          }, delay);
          return id;
        } else {
          t.runTaskForThreadPool(fn);
          return null;
        }
      },
      close() {},
      cancel(id) {
        if (id) uiTimer.clearTimeout(id);
      },
    };
  },
};
function newSingleScheduler() {
  const th = t.start(() => {
    setInterval(() => {}, 1000);
  });
  th.waitFor();
  return {
    th,
    bid: 0,
    schedule(work, delay, state) {
      const action = new SchedulerAction(this, work);
      return action.schedule(state, delay);
    },
    now() {
      return Date.now();
    },
    getexecutor() {
      return {
        th: this.th,
        run(fn, delay) {
          return this.th.setTimeout(fn, delay);
        },
        close() {},
        cancel(id) {
          this.th.clearTimeout(id);
        },
      };
    },
    recycle(){
      this.th.interrupt()
    }
  };
}

exports.ioScheduler = ioScheduler;
exports.mainScheduler = mainScheduler;
exports.uiScheduler = uiScheduler;
exports.workScheduler = workScheduler;
exports.newSingleScheduler = newSingleScheduler;
