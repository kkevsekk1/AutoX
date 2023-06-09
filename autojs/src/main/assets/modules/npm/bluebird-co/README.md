bluebird-co
=============
[![NPM Version][npm-image]][npm-url]
[![NPM Downloads][downloads-image]][npm-url]
[![MIT License][license-image]][npm-url]
[![Build Status][build-image]][build-link]

A set of high performance yield handlers for Bluebird coroutines.

# Description
bluebird-co is a reimplementation of [tj/co](https://github.com/tj/co) generator coroutines using [bluebird](https://github.com/petkaantonov/bluebird), [Bluebird.coroutine](http://bluebirdjs.com/docs/api/promise.coroutine.html) and [Bluebird.coroutine.addYieldHandler](http://bluebirdjs.com/docs/api/promise.coroutine.addyieldhandler.html) to insert a yield handler that can transform all the same yieldable value types as tj/co and more.

[Yieldable Types](#yieldable-types) include arrays of promises, objects with promises as properties, thunks, other generators, and even ES6 iterables. Plus bluebird-co allows for additional yield handlers to be added that work together in combination with all the existing yield handlers.

Combined with [Babel's `async-to-module-method`](http://babeljs.io/docs/plugins/transform-async-to-module-method/) (or `bluebirdCoroutines` in Babel 5) transformer, you can write easy and comprehensive `async/await` functions.

# Quickstart

To install:
`npm install bluebird-co`

Ensure Bluebird is installed:
`npm install bluebird@3`

# Performance
Squeezing the most performance out of every asynchronous operation was a high priority for bluebird-co, and as a result it is much faster than tj/co in essentially every scenario.

[See here for detailed benchmarks](https://github.com/novacrazy/bluebird-co/tree/master/benchmark)

# Usage
bluebird-co includes Bluebird as a [peer dependency](https://docs.npmjs.com/files/package.json#peerdependencies) so that it will use any already installed instance of Bluebird, making it easier to bootstrap and integrate.

Using automatic bootstrapping:
```javascript
require('bluebird-co');
```

and done.

Alternatively, manually adding the yield handler to Bluebird:

```javascript
var Promise     = require('bluebird'),
    bluebird_co = require('bluebird-co/manual');

Promise.coroutine.addYieldHandler(bluebird_co.toPromise);

var fn = Promise.coroutine(function*(){
    //do stuff
});

fn().then(...);
```

In the automatic bootstrapping version, it actually executes the same first four lines of code as above to add the yield handler. Bluebird-co provides manual bootstrapping for control over the process if desired.

### Usage with Babel 6:

Babel 6 provides the [transform-async-to-module-method](http://babeljs.io/docs/plugins/transform-async-to-module-method/) plugin which can pass a generator to a function to convert it to an asynchronous coroutine. Using bluebird-co instead of the default Bluebird install will automatically bootstrap the yield handler while remaining completely transparent.

**.babelrc file**
```javascript
{
    "plugins": [
        ["transform-async-to-module-method", {
            "module": "bluebird-co",
            "method": "coroutine"
        }]
    ]
}
```

**ES7 file to be transformed**
```javascript
async function fn() {
    //do stuff
}

fn().then(...);
```

# Example coroutines
**Note**: bluebird-co has to be added to Bluebird via automatic bootstrapping or manual addition before these snippets can work.

```javascript
var Promise = require('bluebird');
var fs = Promise.promisifyAll(require('fs'));

var myAsyncFunction = Promise.coroutine(function*() {
    var results = yield [Promise.delay( 10 ).return( 42 ),
                         readFileAsync( 'index.js', 'utf-8' ),
                         [1, Promise.resolve( 12 )]];

    console.log(results); //[42, "somefile contents", [1, 12]]
});

myAsyncFunction().then(...);
```

### ES7 version
```javascript
import Promise from 'bluebird';
import {readFile} from 'fs';

let readFileAsync = Promise.promisify(readFile);

async function myAsyncFunction() {
    let results = await [Promise.delay( 10 ).return( 42 ),
                         readFileAsync( 'index.js', 'utf-8' ),
                         [1, Promise.resolve( 12 )]];

    console.log(results); //[42, "somefile contents", [1, 12]]
}

myAsyncFunction().then(...);
```

### tj/co drop-in replacement
```javascript
import {co} from 'bluebird-co';

co(...);
co.wrap(...);
```

##### For more examples, see the [tj/co README](https://github.com/tj/co/blob/master/Readme.md#examples) and the [Bluebird Coroutines API](http://bluebirdjs.com/docs/api/promise.coroutine.html).


# Yieldable Types

* Promises
* Arrays
* Objects
* Generators and GeneratorFunctions
* Iterables (like `new Set([1, 2, 3]).values()`)
* Functions (as Thunks)
* Custom data types via [`.addYieldHandler(fn)`#addyieldhandlerfn--function
* Any combination or nesting of the above.

# Custom yieldable types
It may become desirable to add custom yield handling for types not listed above based on the needs of a certain application. To make this easy, bluebird-co provides an analogue to Bluebird's [Bluebird.coroutine.addYieldHandler](http://bluebirdjs.com/docs/api/promise.coroutine.addyieldhandler.html) that works together in combination with the above yield handlers.

To do this, bluebird-co provides the [`.coroutine.addYieldHandler(fn)`](#addyieldhandlerfn--function) function, or just [`.addYieldHandler(fn)`](#addyieldhandlerfn--function) for short. The first is for strict compatibility with Bluebird.

Example of automatically fetching model data by yielding the model instance:
```javascript
import {coroutine} from 'bluebird-co';

class MyModel {
    async fetch() {
        //do stuff
        return data;
    }
}

coroutine.addYieldHandler(function(value) {
    if(value instanceof MyModel) {
        return value.fetch();
    }
});

async function test() {
    let model = new MyModel();

    let data = await model; //calls model.fetch() and waits on it.
}
```

Additionally, you can even access the array of yield handlers manually if you ever need to remove one, like so:
```javascript
console.log(coroutine.yieldHandlers); //array of functions
```

#### Caveats:

Although this works for most classes and even null, it will **NOT** work if the class inherits from `Object` or if `Object` is in the prototype chain for the value's constructor. If it inherits from `Object`, it will be considered an `Object` instance and processed like any other object. Values without any `constructor` property will be considered an `Object`, as well.

# API

All functions and properties listed below are exported by the `bluebird-co` module.

-----
##### `.toPromise(value : any)` -> `Promise<any> | any`

`toPromise` is the central function of bluebird-co. It takes any of the supported yieldable types (and those added via [`.addYieldHandler`](#addyieldhandlerfn--function)), and attempts to convert it to a Promise. Any Promises within the value are resolved before the returned Promise resolves.

In the event that the given value cannot be transformed into a Promise, like when it is not in the possible yieldable types, the original value is returned. When used in conjunction with Bluebird coroutines, Bluebird will throw an error because the value is not a Promise instance.

-----
##### `.addYieldHandler(fn : Function)`
**alias**: `.coroutine.addYieldHandler(fn : Function)`

Very similar to [Bluebird.coroutine.addYieldHandler](http://bluebirdjs.com/docs/api/promise.coroutine.addyieldhandler.html), `.addYieldHandler` allows custom types to be processed by bluebird-co in conjunction with any other yieldable types, even other custom yield handlers. This includes nested yieldable types.

Aside from the [caveats](#caveats) of `Object` types when using custom yield handlers, any other type or value can be handled, even null, undefined, Symbols, anything. However, built-in yield handlers cannot be overridden.

See the above section on [Custom yieldable types](#custom-yieldable-types) for an example.

-----
##### `.coroutine(gfn : `[`GeneratorFunction`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/GeneratorFunction)`)` -> `Function`
**alias**: `.wrap(gfn : `[`GeneratorFunction`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/GeneratorFunction)`)` -> `Function`

**alias**: `.co.wrap(gfn : `[`GeneratorFunction`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/GeneratorFunction)`)` -> `Function`

This calls [Bluebird.coroutine](http://bluebirdjs.com/docs/api/promise.coroutine.html) and returns the resulting function. When called, the returned function will return a Promise.

The `.wrap` alias is provided to be a drop-in replacement for `co.wrap`.

-----
##### `.execute(gfn : `[`GeneratorFunction`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/GeneratorFunction)`|`[`Generator`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Generator)`, ...args : any[])` -> `Promise<any>`
**alias**: `.co(gfn : `[`GeneratorFunction`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/GeneratorFunction)`|`[`Generator`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Generator)`, ...args : any[])` -> `Promise<any>`

This calls [`.coroutine`](), then invokes the resulting function with the arguments provided, or just runs the generator object directly.

It is meant as a drop in replacement for tj/co `co`, like so:

```javascript
//import {co} from 'bluebird-co';
var co = require('bluebird-co').co;

function* do_stuff(num) {
    return num;
}

co(do_stuff, 10).then(function(result){
    console.log(result); //10
});
```

-----
##### `.coroutine.yieldHanlders` -> `Array<Function>`

Exposes all custom yield handlers that have been added.

bluebird-co expects this to be an array, so don't overwrite it with something silly.

-----
##### `.isThenable(value : any)` -> `boolean`
**alias**: `.isPromise(value : any)` -> `boolean`

```javascript
function isThenable(value) {
    return value && typeof value.then === 'function';
}
```

-----
##### `.isGenerator(value : any)` -> `boolean`

Checks if the value is a generator instance with `.next` and `.throw`.

-----
##### `.isGeneratorFunction(value : any)` -> `boolean`

Checks if the value is a [`GeneratorFunction`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/GeneratorFunction).

-----
# Changelog

##### 2.2.0
* Allow `.co` to accept generators and generator functions like `tj/co` does. (thanks [pkaminski](https://github.com/pkaminski))

##### 2.1.2
* Add `.co.wrap` alias for `.wrap`

##### 2.1.1
* Add simple quickstart section to README
* Add usage docs with Babel 6

##### 2.1.0
* Added `.execute`/`.co` functions.
* ~~(**BUILD**)~~(**FIXED**) As of this release, the build is failing only because Babel runtime is screwed up.

##### 2.0.0
* Improve docs
* Upgrade to Bluebird 3.0
* Upgrade to Babel 6 for build system
* (**MAJOR**) Change behavior of classes that inherit from `Object`
* Small internal improvements

##### 1.3.1 - 1.3.2
* Significantly improve performance of iterables.

##### 1.3.0
* Basic support for Iterables

##### 1.2.0
* Allow manual addition of the yield handler via requiring `bluebird-co/manual`
* Exposed `toPromise` function in extra API

##### 1.1.2 - 1.1.12
* Optimizations and bugfixes

##### 1.1.1
* Don't export `isNativeObject`, because it isn't generic enough to use in most places, only internally under the right circumstances.

##### 1.1.0
* Differentiate between native objects and class instances. Fixes `addYieldHandler` functionality when used with class instances, but does not accept class instances as objects when there is not a handler for them.

##### 1.0.0 - 1.0.5
* Initial releases, documentation and bugfixes.

-----
## License

The MIT License (MIT)

Copyright (c) 2015 Aaron Trent

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

[npm-image]: https://img.shields.io/npm/v/bluebird-co.svg?style=flat
[npm-url]: https://npmjs.org/package/bluebird-co
[downloads-image]: https://img.shields.io/npm/dm/bluebird-co.svg?style=flat
[build-image]: https://travis-ci.org/novacrazy/bluebird-co.svg?branch=master
[build-link]: https://travis-ci.org/novacrazy/bluebird-co
[license-image]: https://img.shields.io/npm/l/bluebird-co.svg?style=flat
