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

function makeObject( size ) {
    var result = {};

    var i = -1;

    while( ++i < size ) {
        result[i] = _bluebird2.default.resolve( i );
    }

    return result;
}

suite( 'very small objects (2 keys)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield makeObject( 2 );
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield makeObject( 2 );
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield makeObject( 2 );
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

suite( 'small objects (10 keys)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield makeObject( 10 );
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield makeObject( 10 );
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield makeObject( 10 );
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

suite( 'large objects (2000 keys)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield makeObject( 2000 );
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield makeObject( 2000 );
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield makeObject( 2000 );
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

suite( 'huge objects (10000 keys)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield makeObject( 10000 );
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield makeObject( 10000 );
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield makeObject( 10000 );
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
