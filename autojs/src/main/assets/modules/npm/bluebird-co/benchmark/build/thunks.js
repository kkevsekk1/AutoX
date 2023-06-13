'use strict';

var _bluebird = require( 'bluebird' );

var _bluebird2 = _interopRequireDefault( _bluebird );

var _ = require( '../../' );

var _2 = _interopRequireDefault( _ );

var _co = require( '../co' );

var _co2 = require( 'co' );

function _interopRequireDefault( obj ) {
    return obj && obj.__esModule ? obj : {default: obj};
}

/**
 * Created by Aaron on 7/11/2015.
 */

function get( val ) {
    return function( done ) {
        done( null, val );
    };
}

var args = new Array( 3000 );

var i = 0;

while( ++i < 3000 ) {
    args[i] = i;
}

args[0] = null;

suite( 'simple thunks (1 argument)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield function( done ) {
            done( null, 10 );
        };
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield function( done ) {
            done( null, 10 );
        };
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield function( done ) {
                done( null, 10 );
            };
        } );
        return function bluebird_version() {
            return ref.apply( this, arguments );
        };
    }();

    bench( 'co', function( next ) {
        co_version().then( next, console.error );
    } );

    bench( 'co with bluebird promises', function( next ) {
        cob_version().then( next, console.error );
    } );

    bench( 'bluebird-co', function( next ) {
        bluebird_version().then( next, console.error );
    } );
} );

suite( 'simple thunks (3 arguments)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield function( done ) {
            done( null, 1, 2, 3 );
        };
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield function( done ) {
            done( null, 1, 2, 3 );
        };
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield function( done ) {
                done( null, 1, 2, 3 );
            };
        } );
        return function bluebird_version() {
            return ref.apply( this, arguments );
        };
    }();

    bench( 'co', function( next ) {
        co_version().then( next, console.error );
    } );

    bench( 'co with bluebird promises', function( next ) {
        cob_version().then( next, console.error );
    } );

    bench( 'bluebird-co', function( next ) {
        bluebird_version().then( next, console.error );
    } );
} );

suite( 'thunks with many arguments (30 arguments)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield function( done ) {
            done( null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 );
        };
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield function( done ) {
            done( null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 );
        };
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield function( done ) {
                done( null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 1, 2, 3, 4, 5, 6, 7, 8, 9,
                    10 );
            };
        } );
        return function bluebird_version() {
            return ref.apply( this, arguments );
        };
    }();

    bench( 'co', function( next ) {
        co_version().then( next, console.error );
    } );

    bench( 'co with bluebird promises', function( next ) {
        cob_version().then( next, console.error );
    } );

    bench( 'bluebird-co', function( next ) {
        bluebird_version().then( next, console.error );
    } );
} );

suite( 'thunks with stupidly many arguments (3000 arguments)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield function( done ) {
            done.apply( null, args );
        };
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield function( done ) {
            done.apply( null, args );
        };
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield function( done ) {
                done.apply( null, args );
            };
        } );
        return function bluebird_version() {
            return ref.apply( this, arguments );
        };
    }();

    bench( 'co', function( next ) {
        co_version().then( next, console.error );
    } );

    bench( 'co with bluebird promises', function( next ) {
        cob_version().then( next, console.error );
    } );

    bench( 'bluebird-co', function( next ) {
        bluebird_version().then( next, console.error );
    } );
} );
