
module.exports = function (runtime, global) {
    let Base64 = android.util.Base64;

    function $base64() {

    }

    $base64.encode = function (str, encoding) {
        let bytes;
        if(encoding) {
            bytes = new java.lang.String(str).getBytes(encoding);
        } else {
            bytes = new java.lang.String(str).getBytes();
        }
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    $base64.decode = function (str, encoding) {
         let bytes = Base64.decode(str, Base64.NO_WRAP);
         if(encoding) {
            return String(new java.lang.String(bytes, encoding));
         } else {
            return String(new java.lang.String(bytes));
         }
    }

    return $base64;
}