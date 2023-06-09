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
 * Created by Aaron on 7/19/2015.
 */

suite( 'raw promises', function() {
    set( 'delay', 0 );
    set( 'mintime', 1750 );

    var co_version = (0, _co2.wrap)( function* () {
        return yield _bluebird2.default.resolve( 1 );
    } );

    var cob_version = (0, _co.wrap)( function* () {
        return yield _bluebird2.default.resolve( 1 );
    } );

    var bluebird_version = function() {
        var ref = (0, _bluebird.coroutine)( function* () {
            return yield _bluebird2.default.resolve( 1 );
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
