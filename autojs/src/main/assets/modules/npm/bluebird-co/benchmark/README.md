Benchmarks
==========

I thought it could be useful to some people to compare the performance of bluebird-co versus tj/co for some common tasks.

Pull requests for new benchmarks are always welcome.

## Index
- [Results](#results)
    - [Promises](#promises)
    - [Arrays](#arrays)
    - [Objects](#objects)
    - [Generators](#generators)
    - [Iterables](#iterables)
    - [Thunks](#thunks)
    - [Error handling](#error-handling)

## Results

Tested with io.js 2.5.0 on Windows 8, Intel i5-4690K at 3.9GHz with 32GB of memory.

### Promises

The most simple yieldable type.

Example:
```javascript
async function() {
    let res = await Promise.resolve(1);

    console.log(res); //1
}
```

| Description  | Library                   | Op/s       | %    |
|--------------|---------------------------|-----------:|-----:|
| raw promises | co                        | 160,725.51 | 17%  |
|              | co with bluebird promises | 489,315.71 | 53%  |
|              | bluebird-co               | 924,237.12 | 100% |

### Arrays

Example:
```javascript
async function() {
    let res = await [asyncFunc1(), asyncFunc2(), somePromise, 42];
}
```

| Description                    | Library                   | Op/s       | %    |
|--------------------------------|---------------------------|-----------:|-----:|
| very short arrays (2 elements) | co                        | 54,917.51  | 11%  |
|                                | co with bluebird promises | 321,764.27 | 64%  |
|                                | bluebird-co               | 502,237.13 | 100% |
| short arrays (10 elements)     | co                        | 20,414.90  | 6%   |
|                                | co with bluebird promises | 236,008.58 | 65%  |
|                                | bluebird-co               | 365,749.26 | 100% |
| long arrays (2000 elements)    | co                        | 98.65      | 2%   |
|                                | co with bluebird promises | 3,947.28   | 66%  |
|                                | bluebird-co               | 5,970.67   | 100% |
| huge arrays (10000 elements)   | co                        | 9.84       | 1%   |
|                                | co with bluebird promises | 784.31     | 66%  |
|                                | bluebird-co               | 1,189.25   | 100% |

### Objects

Example:
```javascript
async function() {
    let res = await {
        a: asyncFunc1(),
        b: asyncFunc2(),
        c: somePromise,
        d: 42
    };
}
```

| Description                 | Library                   | Op/s       | %    |
|-----------------------------|---------------------------|-----------:|-----:|
| very small objects (2 keys) | co                        | 41,523.27  | 20%  |
|                             | co with bluebird promises | 143,294.58 | 68%  |
|                             | bluebird-co               | 211,335.89 | 100% |
| small objects (10 keys)     | co                        | 15,833.65  | 16%  |
|                             | co with bluebird promises | 61,377.07  | 62%  |
|                             | bluebird-co               | 99,776.80  | 100% |
| large objects (2000 keys)   | co                        | 81.89      | 5%   |
|                             | co with bluebird promises | 784.44     | 46%  |
|                             | bluebird-co               | 1,719.45   | 100% |
| huge objects (10000 keys)   | co                        | 9.04       | 3%   |
|                             | co with bluebird promises | 148.29     | 43%  |
|                             | bluebird-co               | 343.88     | 100% |

### Generators

Example:
```javascript
function* test(iterations){
    let i = 0;

    while(i++ < iterations) {
        yield Promise.resolve(i);
    }
}

async function() {
    await test(100);
}

//what even is this
function* complex_generator( iterations ) {
    let test3 = new Array( iterations );

    for( let i = 0; i < iterations; i++ ) {
        test3[i] = Promise.resolve( i );
    }

    for( let i = 0; i < iterations; i++ ) {
        yield [yield Promise.resolve( i ), {
            test:  yield Promise.resolve( i ),
            test2: Promise.resolve( i + 1 ),
            test3: test3
        }];
    }
}
```

| Description                                     | Library                   | Op/s       | %    |
|-------------------------------------------------|---------------------------|-----------:|-----:|
| simple generators (10 iterations)               | co                        | 78,815.58  | 44%  |
|                                                 | co with bluebird promises | 140,148.57 | 79%  |
|                                                 | bluebird-co               | 177,858.87 | 100% |
| long-running generators (1000 iterations)       | co                        | 2,651.80   | 94%  |
|                                                 | co with bluebird promises | 2,707.21   | 96%  |
|                                                 | bluebird-co               | 2,825.15   | 100% |
| very long-running generators (10000 iterations) | co                        | 275.11     | 95%  |
|                                                 | co with bluebird promises | 275.76     | 95%  |
|                                                 | bluebird-co               | 288.95     | 100% |
| complex generators (150 iterations)             | co                        | 10.91      | 1%   |
|                                                 | co with bluebird promises | 358.10     | 49%  |
|                                                 | bluebird-co               | 727.59     | 100% |

### Iterables

Iterables are a generalization of generators for sequences instead of functions. The ES6 `Set`, `Map`, `TypedArray` and others use iterables to lazily traverse their contents.

Example:
```javascript
let mySet = new Set([1, 2, 3, 4]);

let values = mySet.values();

let cur = values.next();

//Prints out 1 2 3 4
while(!cur.done) {
    console.log(cur.value);

    cur = values.next();
}
```

bluebird-co allows automatic traversal of these iterables in a similar way to [`Array.from`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/from), but will also automatically resolve any asynchronous values along the way.

Example:
```javascript
let mySet = new Set( [Promise.resolve( 1 ), 2, [Promise.resolve( 3 ), 4], 5] );

async function() {
    let values = await mySet.values();

    console.log(values); //[1, 2, [3, 4], 5]
}
```

| Description                              | Library     | Op/s       |
|------------------------------------------|-------------|-----------:|
| very short iterables (Set of 2 elements) | co          | N/A        |
|                                          | bluebird-co | 370,524.28 |
| short iterables (Set of 10 elements)     | co          | N/A        |
|                                          | bluebird-co | 240,840.02 |
| long iterables (Set of 2000 elements)    | co          | N/A        |
|                                          | bluebird-co | 3,223.65   |
| huge iterables (Set of 10000 elements)   | co          | N/A        |
|                                          | bluebird-co | 611.33     |

The ES6 `Map` also uses iterables/iterators for `.keys()`, `.entries()` and `.values()`

*NOTE*: If you're using Babel or a version of Node/io.js that natively supports `for..of` loops, they can support iterating through iterables natively instead of having to convert them to an array first, but they will not automatically resolve asynchronous values in those iterables.

### Thunks

Example:
```javascript
function get(value) {
    return function(done) {
        done(null, value); //single argument
    }
}

async function() {
    let value = await get(10);

    console.log(value); //10
}
```

| Description                                          | Library                   | Op/s       | %    |
|------------------------------------------------------|---------------------------|-----------:|-----:|
| simple thunks (1 argument)                           | co                        | 213,076.30 | 34%  |
|                                                      | co with bluebird promises | 334,518.06 | 54%  |
|                                                      | bluebird-co               | 617,680.78 | 100% |
| simple thunks (3 arguments)                          | co                        | 144,597.94 | 30%  |
|                                                      | co with bluebird promises | 194,699.89 | 41%  |
|                                                      | bluebird-co               | 479,569.37 | 100% |
| thunks with many arguments (30 arguments)            | co                        | 81,471.67  | 18%  |
|                                                      | co with bluebird promises | 97,033.90  | 21%  |
|                                                      | bluebird-co               | 464,390.60 | 100% |
| thunks with stupidly many arguments (3000 arguments) | co                        | 1,735.43   | 3%   |
|                                                      | co with bluebird promises | 1,743.87   | 3%   |
|                                                      | bluebird-co               | 56,539.23  | 100% |

### Error handling

Note: This one is here for full disclosure that bluebird-co is indeed slower when it comes to error handling. This is partly because errors have to go through the `Bluebird.coroutine` circuitry instead of being passed/thrown directly back like co does. However, unless you plan on throwing fifty thousand errors per second, it shouldn't ever be an issue.

Example:
```javascript
//Top level error
async function() {
    await undefined;
}

//nested error
async function() {
    await function*() {
        yield undefined;
    }
}

//deep error
async function() {
    let i = 0;

    await function*() {
        if(i++ > 2000) throw new Error();

        yield Promise.resolve(i);
    }
}
```

| Description                                 | Library                   | Op/s       | %    |
|---------------------------------------------|---------------------------|-----------:|-----:|
| top level error handling                    | co                        | 126,256.56 | 100% |
|                                             | co with bluebird promises | 90,805.67  | 72%  |
|                                             | bluebird-co               | 84,419.54  | 67%  |
| nested error handling                       | co                        | 43,982.90  | 100% |
|                                             | co with bluebird promises | 36,999.50  | 84%  |
|                                             | bluebird-co               | 41,840.51  | 95%  |
| deep error handling (after 2000 iterations) | co                        | 1,061.91   | 87%  |
|                                             | co with bluebird promises | 1,188.91   | 98%  |
|                                             | bluebird-co               | 1,214.72   | 100% |
