/****
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Aaron Trent
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 ****/
'use strict';

exports.__esModule = true;
exports.isPromise  = exports.co = exports.wrap = void 0;
exports.coroutine           = coroutine;
exports.execute             = execute;
exports.isThenable          = isThenable;
exports.isGenerator         = isGenerator;
exports.isGeneratorFunction = isGeneratorFunction;
exports.toPromise           = toPromise;
exports.addYieldHandler     = addYieldHandler;

var _bluebird = require( 'bluebird' );

var _bluebird2 = _interopRequireDefault( _bluebird );

function _interopRequireDefault( obj ) {
    return obj && obj.__esModule ? obj : {default: obj};
}

coroutine.yieldHandlers = [];
/**
 * Created by Aaron on 7/3/2015.
 */

coroutine.addYieldHandler = addYieldHandler;

function coroutine( fn ) {
    return _bluebird2.default.coroutine( fn );
}

function execute( fn ) {
    for( var _len = arguments.length, args = Array( _len > 1 ? _len - 1 : 0 ), _key = 1; _key < _len; _key++ ) {
        args[_key - 1] = arguments[_key];
    }

    if( isGenerator( fn ) ) {
        return resolveGenerator( fn );
    } else if( isGeneratorFunction( fn ) ) {
        return coroutine( fn ).apply( this, args );
    } else {
        var value = fn.apply( this, args );
        if( isGenerator( value ) ) {
            return resolveGenerator( value );
        } else {
            throw new Error( 'Can\'t make a coroutine from: ' + value );
        }
    }
}

var wrap = exports.wrap = coroutine;
var co = exports.co = execute;

co.wrap = wrap; //Simple alias that makes it like tj/co

function isThenable( obj ) {
    return obj && typeof obj.then === 'function';
}

var isPromise = exports.isPromise = isThenable;

function isGenerator( obj ) {
    return 'function' === typeof obj.next && 'function' === typeof obj.throw;
}

function isGeneratorFunction( obj ) {
    var constructor = obj.constructor;

    if( !constructor ) {
        return false;
    } else if( 'GeneratorFunction' === constructor.name || 'GeneratorFunction' === constructor.displayName ) {
        return true;
    } else {
        var prototype = constructor.prototype;

        return 'function' === typeof prototype.next && 'function' === typeof prototype.throw;
    }
}

function objectToPromise( obj, constructor ) {
    var keys   = Object.keys( obj );
    var length = keys.length | 0;

    var result = new constructor();
    var values = new Array( length );

    var i = -1;

    while( ++i < length ) {
        var key = keys[i];

        result[key] = void 0;

        values[i] = toPromise.call( this, obj[key] );
    }

    return _bluebird2.default.all( values ).then( function( res ) {
        var i = res.length | 0;

        while( --i >= 0 ) {
            result[keys[i]] = res[i];
        }

        return result;
    } );
}

function resolveGenerator( gen ) {
    return new _bluebird2.default( function( resolve, reject ) {
        function next( ret ) {
            if( ret.done ) {
                resolve( ret.value );
            } else {
                var value = ret.value;

                if( isThenable( value ) ) {
                    value.then( onFulfilled, onRejected );

                    return null;
                } else {
                    value = toPromise.call( this, value );

                    if( isThenable( value ) ) {
                        value.then( onFulfilled, onRejected );

                        return null;
                    } else {
                        onRejected(
                            new TypeError( 'You may only yield a function, promise, generator, array, or object, but the following object was passed: "' +
                                           ret.value + '"' ) );
                    }
                }
            }
        }

        function onFulfilled( res ) {
            try {
                next( gen.next( res ) );
            } catch( e ) {
                reject( e );
            }

            return null;
        }

        function onRejected( err ) {
            try {
                next( gen.throw( err ) );
            } catch( e ) {
                reject( e );
            }

            return null;
        }

        onFulfilled();
    } );
}

function arrayFromIterable( iter ) {
    var results = [];
    var ret     = iter.next();

    while( !ret.done ) {
        results.push( ret.value );

        ret = iter.next();
    }

    return results;
}

var arrayFrom = typeof Array.from === 'function' ? Array.from : arrayFromIterable;

function arrayToPromise( value ) {
    var length = value.length | 0;

    var results = new Array( length );

    while( --length >= 0 ) {
        results[length] = toPromise.call( this, value[length] );
    }

    return _bluebird2.default.all( results );
}

//This is separated out so it can be optimized independently to the calling function.
function processThunkArgs( args ) {
    var length = args.length | 0;

    if( length >= 3 ) {
        var res = new Array( --length );

        for( var i = 0; i < length; ) {
            res[i] = args[++i]; //It's a good thing this isn't undefined behavior in JavaScript
        }

        return res;
    }

    return args[1];
}

function thunkToPromise( value ) {
    var _this = this;

    return new _bluebird2.default( function( resolve, reject ) {
        try {
            value.call( _this, function( err ) {
                if( err ) {
                    reject( err );
                } else {
                    resolve( processThunkArgs( arguments ) );
                }
            } );
        } catch( err ) {
            reject( err );
        }
    } );
}

function toPromise( value ) {
    if( typeof value === 'object' && !!value ) {
        if( typeof value.then === 'function' ) {
            return value;
        } else if( Array.isArray( value ) ) {
            return arrayToPromise.call( this, value );
        } else if( 'function' === typeof value.next ) {
            if( 'function' === typeof value.throw ) {
                return resolveGenerator.call( this, value );
            } else {
                return arrayToPromise.call( this, arrayFrom( value ) );
            }
        } else {
            /*
             * If there is no constructor, default to Object, because no constructor means it was
             * created in weird circumstances.
             * */

            var _value$constructor = value.constructor,
                _constructor       = _value$constructor === void 0 ? Object : _value$constructor;

            /*
             * This is really annoying, as there is no possible way to determine whether `value` is an instance of
             * an Object, or is a class that extends Object, given Babel 6. It seems possible in Babel 5.
             * I'm not sure if this was an intentional change, but for the sake of forward compatibility, this will
             * consider anything that also inherits from Object as an object.
             * */

            if( _constructor === Object || Object.isPrototypeOf( _constructor ) ) {
                return objectToPromise.call( this, value, _constructor );
            }
        }
    } else if( typeof value === 'function' ) {
        if( isGeneratorFunction( value ) ) {
            return _bluebird2.default.coroutine( value ).call( this );
        } else {
            return thunkToPromise.call( this, value );
        }
    }

    /*
     * Custom yield handlers allow bluebird-co to be extended similarly to bluebird yield handlers, but have the
     * added benefit of working with all the other bluebird-co yield handlers automatically.
     * */
    for( var _iterator = coroutine.yieldHandlers, _isArray = Array.isArray( _iterator ), _i = 0, _iterator = _isArray ?
                                                                                                             _iterator :
                                                                                                             _iterator[Symbol.iterator](); ; ) {
        var _ref;

        if( _isArray ) {
            if( _i >= _iterator.length ) {
                break;
            }
            _ref = _iterator[_i++];
        } else {
            _i = _iterator.next();
            if( _i.done ) {
                break;
            }
            _ref = _i.value;
        }

        var handler = _ref;

        var res = handler.call( this, value );

        if( isThenable( res ) ) {
            return res;
        }
    }

    return value;
}

function addYieldHandler( handler ) {
    if( typeof handler !== 'function' ) {
        throw new TypeError( 'yield handler is not a function' );
    } else {
        coroutine.yieldHandlers.push( handler );
    }
}
