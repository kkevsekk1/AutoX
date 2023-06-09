'use strict';

var _bluebird = require( 'bluebird' );

var _bluebird2 = _interopRequireDefault( _bluebird );

var _set = require( 'babel-runtime/core-js/set' );

var _set2 = _interopRequireDefault( _set );

var _ = require( '../../' );

var _2 = _interopRequireDefault( _ );

function _interopRequireDefault( obj ) {
    return obj && obj.__esModule ? obj : {default: obj};
}

/**
 * Created by Aaron on 7/17/2015.
 */

function makeSet( length ) {
    var res = new Array( length );
    var i = -1;

    while( ++i < length ) {
        res[i] = i;
    }

    return new _set2.default( res );
}

suite( 'very short iterables (Set of 2 elements)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield makeSet( 2 ).values();
        } );
        return function bluebird_version() {
            return ref.apply( this, arguments );
        };
    }();

    bench( 'bluebird-co', function( next ) {
        bluebird_version().then( next, console.error );
    } );
} );

suite( 'short iterables (Set of 10 elements)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield makeSet( 10 ).values();
        } );
        return function bluebird_version() {
            return ref.apply( this, arguments );
        };
    }();

    bench( 'bluebird-co', function( next ) {
        bluebird_version().then( next, console.error );
    } );
} );

suite( 'long iterables (Set of 2000 elements)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield makeSet( 2000 ).values();
        } );
        return function bluebird_version() {
            return ref.apply( this, arguments );
        };
    }();

    bench( 'bluebird-co', function( next ) {
        bluebird_version().then( next, console.error );
    } );
} );

suite( 'huge iterables (Set of 10000 elements)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield makeSet( 10000 ).values();
        } );
        return function bluebird_version() {
            return ref.apply( this, arguments );
        };
    }();

    bench( 'bluebird-co', function( next ) {
        bluebird_version().then( next, console.error );
    } );
} );
