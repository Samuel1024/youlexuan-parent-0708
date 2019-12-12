 //控制层 
app.controller('cartController' ,function($scope,$controller   ,cartService,orderService){
	
	$controller('baseController',{$scope:$scope});//继承


    $scope.findCartList = function () {
        cartService.findCartList().success(
        	function (response) {
				$scope.cartList = response;
				$scope.totalValue = cartService.sum($scope.cartList)



            }
		)
    }
    
    $scope.addGoodsToCart = function (itemId,num) {
		cartService.addGoodsToCart(itemId,num).success(
			function (respons) {
				if(respons.success){
					$scope.findCartList();//刷新列表页
				}else{
					alert(respons.message);
				}
            }
		)
    }
    
    $scope.findAllUserAddress = function () {
    //	alert();
		cartService.findAddressList().success(
			function (response) { //所有的地址
				//绑定和渲染
				$scope.addressList = response;
                //设置默认地址
                for(var i=0;i< $scope.addressList.length;i++){
                    if($scope.addressList[i].isDefault=='1'){
                        $scope.address=$scope.addressList[i];
                        break;
                    }
                }

            }
		)
    }
    
    $scope.selectAddress =function(addr) {
		$scope.address = addr;
    }

    $scope.isSelectedAddress = function (addr) {
		if(addr==$scope.address){
			return true;
		}else {
			return false;
		}
    }

    /**
	 *
	 * 加工支付方式
     * @type {{paymentType: string}}
     */
    $scope.order = {"paymentType":"1"}
    $scope.selectPayType = function (type) {
        $scope.order.paymentType = type;
    }

    $scope.submitOrder = function () {
        $scope.order.receiverAreaName = $scope.address.address;
        $scope.order.receiverMobile  = $scope.address.mobile;
        $scope.order.receiver  = $scope.address.contact;
        orderService.submitOrder($scope.order).success(
            function (response) {
                // 如果是提交成功、判断是否是在线支付
                if(response.success){
                    if($scope.order.paymentType == '1'){
                        location.href = "pay.html";
                    }else {
                        location.href="paysuccess.html";
                    }
                }else{
                    alert(response.message);
                }
            }
        )
    }


    
});	