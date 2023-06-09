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

/**
 * Created by Aaron on 7/11/2015.
 */

function* gen( iterations ) {
    for( var i = 0; i < iterations; i++ ) {
        yield _bluebird2.default.resolve( i );
    }
}

//what even is this
function* gen_complex( iterations ) {
    var test3 = new Array( iterations );

    for( var i = 0; i < iterations; i++ ) {
        test3[i] = _bluebird2.default.resolve( i );
    }

    for( var i = 0; i < iterations; i++ ) {
        yield [
            yield _bluebird2.default.resolve( i ), {
                test:  yield _bluebird2.default.resolve( i ),
                test2: _bluebird2.default.resolve( i + 1 ),
                test3: test3
            }
        ];
    }
}

suite( 'simple generators (10 iterations)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield gen( 10 );
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield gen( 10 );
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield gen( 10 );
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

suite( 'long-running generators (1000 iterations)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield gen( 1000 );
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield gen( 1000 );
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield gen( 1000 );
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

suite( 'very long-running generators (10000 iterations)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield gen( 10000 );
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield gen( 10000 );
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield gen( 10000 );
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

suite( 'complex generators (150 iterations)', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield gen_complex( 150 );
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield gen_complex( 150 );
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield gen_complex( 150 );
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
