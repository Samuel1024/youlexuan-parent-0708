 //控制层 
app.controller('payController' ,function($scope,$controller   ,payService){
	
	$controller('baseController',{$scope:$scope});//继承
	
	$scope.creatNative =function () {
		payService.creatNative().success(
			function (response) {
                new QRious({
                    element:document.getElementById("qrious"),
                    size:250,
                    level:'H',
                    value:response.qrcode
                })

                queryPayStatus(response.out_trade_no);

            }
		)
    }

    queryPayStatus = function(out_trade_no){
	    payService.queryPayStatus(out_trade_no).success(
	        function (response) {
                if(response.success){
                    location.href = "paysuccess.html";
                }else{
                    location.href="payfail.html";
                }
            }
        )
    }



});	