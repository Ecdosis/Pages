/**
 * A simple point class
 * @param x the-coordinate
 * @param y the y-coordinate
 */
function Point(x,y)
{
    this.x = x;
    this.y = y;
    this.print = function() {
        console.log(this.toString());
    };
    this.toString = function() {
        return "x="+this.x+",y="+this.y;
    };
    this.equals = function(pt) {
        return pt!=undefined&&this.x==pt.x&&this.y==pt.y;
    };
}
/**
 * A simple rectangle class
 * @param x the x-position of top-left
 * @param y the y-position of top-left
 * @param width the width of the rect
 * @param height the height of the rect
 */
function Rect( x, y, width, height )
{
    this.width = width;
    this.height = height;
    this.x = x;
    this.y = y;
    this.contains = function( p ) {
        if ( p.x >= this.x && p.x < this.x+this.width )
        {
            if ( p.y >= this.y  && p.y < this.y+this.width )
                return true;
        }
        return false;
    };
    this.print = function(){
        console.log("x="+this.x+",y="+this.y+",width="
        +this.width+",height="+this.height);
    };
}
/**
 * Polygon class
 * @param pts a plain javascript array of x,y objects
 * @param id the id of the canvas we are drawn inside
 */
function Polygon( pts, id )
{
    // flag to save redrawing
    this.drawn = false;
    // the id of the canvas
    this.canvas = "#"+id;
    // radius of control points
    this.radius = 4;
    // colour of control points
    this.controlColour = '#003300';
    // fill colour of polygons
    this.fillColour = "#FF0000";
    // colour of highlighted point
    this.highlightColour = "#00FF00";
    // true if anchored by a click
    this.anchored = false;
    // currently dragged point
    this.dragPt = undefined;
    // currently highlighted point
    this.highlightPt = undefined;
    // array of points
    this.points = new Array();
    for ( var i=0;i<pts.length;i++ )
    {
        var pt = new Point(pts[i].x,pts[i].y);
        this.points[this.points.length] = pt;
    }
    /**
     * Get the points of the polygon
     * @return an array of Point
     */
    this.getPoints = function() {
        return this.points;
    };
    /**
     * Is the given point inside this polygon?
     * @param pt the point to test for
     * @return true if it was else false 
     */
    this.pointInPoly = function(pt) {
        var cn = 0;
        var V = this.points;
        for ( var i=0; i<V.length-1; i++ ) 
        {
            if (((V[i].y <= pt.y) && (V[i+1].y > pt.y))
            || ((V[i].y > pt.y) && (V[i+1].y <=  pt.y))) 
            {
                var vt = (pt.y  - V[i].y) / (V[i+1].y - V[i].y);
                if (pt.x <  V[i].x + vt * (V[i+1].x - V[i].x))
                     ++cn;
            }
        }
        return cn&1;
    };
    /**
     * Draw this polygon in the canvas' context
     */
    this.drawPolygon = function() { 
        if ( !this.drawn )
        {
            var ctx = $(this.canvas)[0].getContext("2d");
            ctx.globalAlpha = 0.35; 
            ctx.fillStyle = this.fillColour;
            ctx.beginPath();
            if ( this.points.length>0 )
                ctx.moveTo(this.points[0].x,this.points[0].y);
            for ( var i=1;i<this.points.length;i++ )
                ctx.lineTo(this.points[i].x,this.points[i].y);
            ctx.closePath();
            ctx.fill();
            this.drawn = true;
        }
    };
    /**
     * Draw the little circles at each corner
     */
    this.drawControlPoints = function() {
        var ctx = $(this.canvas)[0].getContext("2d");
        ctx.globalAlpha = 0.55; 
        for ( var i=0;i<this.points.length;i++ )
        {
            var pt = this.points[i];
            ctx.beginPath();
            if ( pt === this.highlightPt || pt === this.dragPt )
            {
                ctx.strokeStyle = this.highlightColour;
                ctx.lineWidth = 2;
                ctx.strokeRect( pt.x-this.radius, pt.y-this.radius, 
                    this.radius*2, this.radius*2 );
            }
            else
            {
                ctx.arc(pt.x, pt.y, this.radius, 0, 2 * Math.PI, false);
                ctx.lineWidth = 1;
                ctx.strokeStyle = this.controlColour;
                ctx.stroke();
            }
        }
    };
    /**
     * Recomute the bounds
     */
    this.computeBounds = function() {
        var minX = Number.MAX_VALUE;
        var minY = Number.MAX_VALUE;
        var maxX = Number.MIN_VALUE;
        var maxY = Number.MIN_VALUE;
        for ( var i=0;i<this.points.length;i++ )
        {
            var pt = this.points[i];
            if ( pt.x < minX )
                minX = pt.x;
            if ( pt.y < minY )
                minY = pt.y;
            if ( pt.x > maxX )
                maxX = pt.x;
            if ( pt.x >maxY )
                maxY = pt.x;
        }
        this.bounds = new Rect( minX, minY, maxX-minX, maxY-minY );
    };
    this.computeBounds();
    /**
     * Clear the polygon 
     */
    this.erase = function() {
        this.computeBounds();
        var ctx = $(this.canvas)[0].getContext("2d");
        var extra = this.radius+1;
        ctx.globalAlpha = 1.0; 
        ctx.clearRect(this.bounds.x-extra,
            this.bounds.y-extra,
            this.bounds.width+extra*2,
            this.bounds.height+extra*2);
        this.drawn = false;
    };
    /**
     * We just started dragging
     * @param pt the point that is being dragged
     */
    this.startDrag = function(pt) {
        for ( var i=0;i<this.points.length;i++)
        {
            if ( pt.x==this.points[i].x && pt.y==this.points[i].y )
            {
                this.dragPt= this.points[i];
                break;
            }
        }
    };
    /**
     * Are we currently dragging a point?
     * @return true if a point is being dragged
     */
    this.dragging = function() {
        return this.dragPt != undefined;
    };
    /**
     * Redraw the polygon and its control points
     */
    this.redraw = function() {
        this.erase();
        this.drawPolygon();
        this.drawControlPoints();
    };
    /**
     * Reset the polygon to the normal anchored state
     */
    this.endDrag = function() {
        this.dragPt = undefined;
        this.redraw();
    };
    /**
     * Remove the highlighted point
     */
    this.setAnchorNormal = function() {
        this.highlightPt = undefined;
        this.redraw();
    };
    /**
     * Drag the currently dragged point
     * @param pt the new position of dragPt
     * @param qt the quadtree to update
     */
    this.dragTo = function(pt,qt) {
        if ( this.dragPt.x != pt.x || this.dragPt.y != pt.y )
        {
            this.erase();
            qt.removePoint(this.dragPt);
            this.dragPt.x = pt.x;
            this.dragPt.y = pt.y;
            qt.addPoint( this.dragPt );
            this.drawPolygon();
            this.drawControlPoints();
        }
    };
    /**
     * Set the highlight point
     * @param pt the point to highlight for subsquent operations
     */
    this.setHighlightPt = function(pt) {
        for ( var i=0;i<this.points.length;i++ )
        {
            if ( this.points[i].equals(pt) )
            {
                this.highlightPt = this.points[i];
                break;
            }
        }
    };
    /**
     * Delete the current highlight point
     */
    this.deleteHighlightPt = function() {
        for ( var i=0;i<this.points.length;i++ )
        {
            if ( this.points[i].equals(this.highlightPt) )
            {
                this.erase();
                this.points.splice(i,1);
                if ( this.points.length>0 )
                    this.highlightPt = this.points[i%this.points.length];
                this.drawPolygon();
                this.drawControlPoints();
                break;
            }
        }
    };
}

