
function myrandom(min,max){
    return Math.floor(Math.random() * (max - min + 1) ) + min;
}

threads.start(function () {
    console.show();
    console.setTitle("中文","#ff11ee00",30);
    console.setCanInput(false);
    var i=0;
    do {
        console.setLogSize(myrandom(4,20) );
        console.setCanInput(i%2==0);
        i++;
        console.log("i----->"+i);
        sleep(3000);
    } while (true);

});