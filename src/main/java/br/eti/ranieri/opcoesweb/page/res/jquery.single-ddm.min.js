/*
 * Single Drop Down Menu 1.2
 * September 26, 2009
 * Corey Hart @ http://www.codenothing.com
 */
;(function(a,b){a.fn.bgiframe=a.fn.bgiframe?a.fn.bgiframe:a.fn.bgIframe?a.fn.bgIframe:function(){return this};a.fn.singleDropMenu=function(c){return this.each(function(){var h=a(this),g,f,e=a.extend({timer:500,parentMO:b,childMO:b,show:"show",hide:"hide"},c||{},a.metadata?h.metadata():{});h.children("li").bind("mouseover.single-ddm",function(){if(f&&f.data("single-ddm-i")!=a(this).data("single-ddm-i")){d()}else{f=false}a(this).children("a").addClass(e.parentMO).siblings("ul")[e.show]()}).bind("mouseout.single-ddm",function(){f=a(this);g=setTimeout(d,e.timer)}).each(function(j){a(this).data("single-ddm-i",j)}).children("ul").bgiframe();a("li > ul > li",h).bind("mouseover.single-ddm",function(){a("a",this).addClass(e.childMO)}).bind("mouseout.single-ddm",function(){a("a",this).removeClass(e.childMO)});a(document).click(d);function d(){if(f&&g){f.children("a").removeClass(e.parentMO).siblings("ul")[e.hide]();clearTimeout(g);f=false}}})}})(jQuery);
